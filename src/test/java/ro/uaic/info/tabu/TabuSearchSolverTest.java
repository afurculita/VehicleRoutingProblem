package ro.uaic.info.tabu;

import org.junit.jupiter.api.Test;
import ro.uaic.info.Solution;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabuSearchSolverTest {

    @Test
    void findsTheOptimumOfTheTinyInstance() {
        Solution solution = new TabuSearchSolver(TestInstances.tiny(), 2, 50, new Random(1)).solve();
        assertEquals(TestInstances.TINY_OPTIMUM, solution.getCost(), 1e-9);
    }

    @Test
    void improvesOnTheGreedySolutionAndStaysFeasible() {
        VrpInstance instance = TestInstances.pn16k8();
        double greedy = new GreedySolver(instance).solve().getCost();

        Solution solution = new TabuSearchSolver(instance, 5, 2000, new Random(42)).solve();

        assertTrue(solution.getCost() < greedy, "tabu " + solution.getCost() + " should beat greedy " + greedy);
        // Best known value with rounded distances is 435; with real-valued distances ~450.
        assertTrue(solution.getCost() < 460.0, "cost " + solution.getCost());
        assertEquals(Solution.cost(instance, solution.getRoutes()), solution.getCost(), 1e-9);
    }

    @Test
    void isReproducibleForAGivenSeed() {
        VrpInstance instance = TestInstances.pn16k8();
        Solution a = new TabuSearchSolver(instance, 5, 500, new Random(7)).solve();
        Solution b = new TabuSearchSolver(instance, 5, 500, new Random(7)).solve();
        assertEquals(a.getRoutes(), b.getRoutes());
        assertEquals(a.getCost(), b.getCost());
    }

    @Test
    void canStartFromAnyFeasibleSolution() {
        VrpInstance instance = TestInstances.tiny();
        Solution poor = Solution.of(instance, List.of(List.of(1), List.of(2), List.of(3), List.of(4)));

        Solution improved = new TabuSearchSolver(instance, 2, 50, new Random(3)).solveFrom(poor);

        assertEquals(TestInstances.TINY_OPTIMUM, improved.getCost(), 1e-9);
    }

    @Test
    void neverReturnsSomethingWorseThanItsStartingPoint() {
        VrpInstance instance = TestInstances.pn16k8();
        Solution start = new GreedySolver(instance).solve();
        for (int seed = 0; seed < 5; seed++) {
            Solution result = new TabuSearchSolver(instance, 20, 30, new Random(seed)).solveFrom(start);
            assertTrue(result.getCost() <= start.getCost() + 1e-9);
        }
    }

    @Test
    void validatesParameters() {
        VrpInstance instance = TestInstances.tiny();
        assertThrows(IllegalArgumentException.class, () -> new TabuSearchSolver(instance, -1, 10, new Random()));
        assertThrows(IllegalArgumentException.class, () -> new TabuSearchSolver(instance, 5, 0, new Random()));
    }
}
