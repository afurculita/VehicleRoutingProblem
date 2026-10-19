package ro.uaic.info.local;

import org.junit.jupiter.api.Test;
import ro.uaic.info.Solution;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VRPLibReader;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises every move type on real instances and checks, after each applied move, that the
 * incremental bookkeeping of {@link RouteState} (cost, loads, lengths, positions) matches a
 * recomputation from scratch and that the solution stays feasible.
 */
class NeighbourhoodTest {

    private enum Kind { RELOCATE, OR_OPT, SWAP, TWO_OPT, CROSS }

    private static Kind kindOf(Move move) {
        return switch (move) {
            case Move.Relocate m -> m.length() == 1 ? Kind.RELOCATE : Kind.OR_OPT;
            case Move.Swap m -> Kind.SWAP;
            case Move.TwoOpt m -> Kind.TWO_OPT;
            case Move.CrossExchange m -> Kind.CROSS;
        };
    }

    private static void assertConsistent(VrpInstance instance, RouteState state) {
        List<List<Integer>> routes = state.toCustomerRoutes();
        Solution solution = Solution.of(instance, routes); // validates feasibility
        assertEquals(solution.getCost(), state.cost(), 1e-6);
        int customers = 0;
        for (int r = 0; r < state.numberOfRoutes(); r++) {
            List<Integer> route = state.route(r);
            assertEquals(0, route.get(0));
            assertEquals(0, route.get(route.size() - 1));
            int load = 0;
            double length = 0.0;
            for (int p = 1; p < route.size(); p++) {
                length += instance.getDistance(route.get(p - 1), route.get(p));
                int node = route.get(p);
                if (node != 0) {
                    load += instance.getDemand(node);
                    assertEquals(r, state.routeOf(node));
                    assertEquals(p, state.positionOf(node));
                    customers++;
                }
            }
            assertEquals(load, state.load(r));
            assertEquals(length, state.length(r), 1e-6);
            assertTrue(length <= instance.getMaxRouteLength() + 1e-9);
        }
        assertEquals(instance.getNumberOfCustomers(), customers);
        assertTrue(state.firstEmptyRoute() >= 0, "a spare empty route must always exist");
    }

    private static VrpInstance a32() throws IOException {
        return VRPLibReader.read(Path.of("datasets", "small", "A-n32-k5.vrp"));
    }

    @Test
    void descentKeepsBookkeepingConsistentAndEndsLocallyOptimal() throws IOException {
        for (VrpInstance instance : List.of(TestInstances.tiny(), TestInstances.pn16k8(), a32())) {
            Neighbourhood neighbourhood = new Neighbourhood(instance, new CandidateLists(instance, 0));
            RouteState state = new RouteState(instance, new GreedySolver(instance).solve());
            double previous = state.cost();
            Move move;
            int applied = 0;
            while ((move = neighbourhood.best(state, MoveEvaluator.IMPROVING)) != null) {
                double expected = state.cost() + move.delta();
                state.apply(move);
                assertEquals(expected, state.cost(), 1e-6, "delta of " + move);
                assertTrue(state.cost() < previous);
                previous = state.cost();
                assertConsistent(instance, state);
                applied++;
            }
            assertTrue(applied > 0 || instance.getNumberOfCustomers() < 5);
            assertNull(neighbourhood.best(state, MoveEvaluator.IMPROVING));
        }
    }

    @Test
    void randomWalkExercisesEveryMoveTypeWithoutBreakingInvariants() throws IOException {
        VrpInstance instance = a32();
        Neighbourhood neighbourhood = new Neighbourhood(instance, new CandidateLists(instance, 10));
        RouteState state = new RouteState(instance, new GreedySolver(instance).solve());
        Random random = new Random(12345);
        Map<Kind, Integer> seen = new EnumMap<>(Kind.class);

        for (int step = 0; step < 400; step++) {
            // accept everything, rank at random: a random walk through the neighbourhood
            Move move = neighbourhood.best(state, m -> random.nextDouble() - 1.0);
            assertNotNull(move);
            double expected = state.cost() + move.delta();
            state.apply(move);
            assertEquals(expected, state.cost(), 1e-6, "delta of " + move);
            assertConsistent(instance, state);
            seen.merge(kindOf(move), 1, Integer::sum);
        }
        for (Kind kind : Kind.values()) {
            assertTrue(seen.getOrDefault(kind, 0) > 0, "no " + kind + " move was generated: " + seen);
        }
    }

    @Test
    void moveFactoriesRejectNoOpsAndInfeasibleMoves() {
        VrpInstance tiny = TestInstances.tiny();
        Neighbourhood neighbourhood = new Neighbourhood(tiny, new CandidateLists(tiny, 0));
        RouteState state = new RouteState(tiny, List.of(List.of(1, 2), List.of(3), List.of(4)));

        assertNull(neighbourhood.relocate(state, 0, 1, 1, false, 0, 0), "re-inserting at the same place");
        assertNull(neighbourhood.relocate(state, 0, 1, 1, false, 0, 1), "re-inserting at the same place");
        assertNull(neighbourhood.relocate(state, 2, 1, 1, false, 0, 0), "customer 4 does not fit with 1 and 2");
        assertNotNull(neighbourhood.relocate(state, 0, 1, 1, false, 1, 0), "customer 1 fits next to 3");
        assertNull(neighbourhood.swap(state, 0, 1, 0, 2), "adjacent customers of one route");
        assertNull(neighbourhood.swap(state, 0, 1, 2, 1), "swapping 1 and 4 overloads route 0");
        assertNotNull(neighbourhood.swap(state, 0, 1, 1, 1));
        assertNull(neighbourhood.twoOpt(state, 0, 1, 2), "reversing the whole route is a no-op");
    }

    @Test
    void respectsMaximumRouteLength() {
        VrpInstance limited = TestInstances.limited();
        Neighbourhood neighbourhood = new Neighbourhood(limited, new CandidateLists(limited, 0));
        List<List<Integer>> singletons = List.of(List.of(1), List.of(2), List.of(3), List.of(4),
                List.of(5), List.of(6), List.of(7), List.of(8));
        for (List<List<Integer>> start : List.of(singletons, new GreedySolver(limited).solve().getRoutes())) {
            RouteState state = new RouteState(limited, start);
            // descent: segment relocations into short routes are where the limit bites
            Move move;
            while ((move = neighbourhood.best(state, MoveEvaluator.IMPROVING)) != null) {
                state.apply(move);
                assertConsistent(limited, state);
            }
            assertEquals(4, state.toCustomerRoutes().size());
            // then a random walk
            Random random = new Random(7);
            for (int step = 0; step < 200; step++) {
                move = neighbourhood.best(state, m -> random.nextDouble() - 1.0);
                assertNotNull(move);
                state.apply(move);
                assertConsistent(limited, state);
            }
        }
    }
}
