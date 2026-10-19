package ro.uaic.info.local;

import org.junit.jupiter.api.Test;
import ro.uaic.info.Solution;
import ro.uaic.info.VRPLibReader;
import ro.uaic.info.VrpInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwoOptTest {

    /** Four customers on the corners of a square around the depot. */
    private static final VrpInstance SQUARE = VRPLibReader.parse("""
            NAME : square
            DIMENSION : 5
            CAPACITY : 100
            NODE_COORD_SECTION
            1 0 0
            2 1 1
            3 1 -1
            4 -1 -1
            5 -1 1
            DEMAND_SECTION
            1 0
            2 1
            3 1
            4 1
            5 1
            """, false);

    @Test
    void uncrossesARoute() {
        List<Integer> route = new ArrayList<>(List.of(1, 3, 2, 4)); // crosses the square diagonally twice
        double before = Solution.routeCost(SQUARE, route);

        double gain = TwoOpt.improve(SQUARE, route);

        double after = Solution.routeCost(SQUARE, route);
        assertEquals(before - after, gain, 1e-9);
        // The perimeter walk 1 -> 2 -> 3 -> 4 (or its reverse) is optimal.
        assertEquals(2 * Math.sqrt(2) + 3 * 2.0, after, 1e-9);
        assertEquals(List.of(1, 2, 3, 4), route);
    }

    @Test
    void leavesAnOptimalRouteUntouched() {
        List<Integer> route = new ArrayList<>(List.of(1, 2, 3, 4));
        assertEquals(0.0, TwoOpt.improve(SQUARE, route), 1e-12);
        assertEquals(List.of(1, 2, 3, 4), route);
    }

    @Test
    void handlesTrivialRoutes() {
        assertEquals(0.0, TwoOpt.improve(SQUARE, new ArrayList<>()));
        assertEquals(0.0, TwoOpt.improve(SQUARE, new ArrayList<>(List.of(2))));
    }

    @Test
    void improveAllSumsTheGains() {
        List<List<Integer>> routes = List.of(new ArrayList<>(List.of(1, 3, 2, 4)), new ArrayList<>());
        double before = Solution.cost(SQUARE, routes);
        double gain = TwoOpt.improveAll(SQUARE, routes);
        assertEquals(before - Solution.cost(SQUARE, routes), gain, 1e-9);
    }
}
