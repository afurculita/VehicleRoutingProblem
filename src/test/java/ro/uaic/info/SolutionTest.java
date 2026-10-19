package ro.uaic.info;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolutionTest {

    private final VrpInstance tiny = TestInstances.tiny();

    @Test
    void computesCostIncludingDepotLegs() {
        Solution solution = Solution.of(tiny, List.of(List.of(1, 2), List.of(3), List.of(4)));

        assertEquals(12.0 + 6.0 + 8.0, solution.getCost(), 1e-12);
        assertEquals(3, solution.getNumberOfRoutes());
        assertEquals(10, solution.getLoad(tiny, 0));
        assertEquals(6, solution.getLoad(tiny, 2));
    }

    @Test
    void routeCostOfEmptyRouteIsZero() {
        assertEquals(0.0, Solution.routeCost(tiny, List.of()));
        assertEquals(6.0, Solution.routeCost(tiny, List.of(1)), 1e-12);
    }

    @Test
    void dropsEmptyRoutesAndIsImmutable() {
        Solution solution = Solution.of(tiny, List.of(List.of(), List.of(1, 2), List.of(), List.of(3), List.of(4)));

        assertEquals(3, solution.getNumberOfRoutes());
        assertThrows(UnsupportedOperationException.class, () -> solution.getRoutes().add(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> solution.getRoutes().get(0).add(9));
    }

    @Test
    void rejectsUnservedCustomer() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Solution.of(tiny, List.of(List.of(1, 2), List.of(3))));
        assertTrue(e.getMessage().contains("not served"), e.getMessage());
    }

    @Test
    void rejectsCustomerServedTwice() {
        assertThrows(IllegalArgumentException.class,
                () -> Solution.of(tiny, List.of(List.of(1, 2), List.of(3), List.of(4, 1))));
    }

    @Test
    void rejectsOverloadedRoute() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Solution.of(tiny, List.of(List.of(1, 2, 3), List.of(4))));
        assertTrue(e.getMessage().contains("capacity"), e.getMessage());
    }

    @Test
    void rejectsDepotOrUnknownNodesInRoutes() {
        assertThrows(IllegalArgumentException.class,
                () -> Solution.of(tiny, List.of(List.of(0, 1, 2), List.of(3), List.of(4))));
        assertThrows(IllegalArgumentException.class,
                () -> Solution.of(tiny, List.of(List.of(1, 2), List.of(3), List.of(4), List.of(5))));
    }

    @Test
    void formatListsRoutesAndBestValue() {
        Solution solution = Solution.of(tiny, List.of(List.of(1, 2), List.of(3), List.of(4)));
        String text = solution.format();

        assertTrue(text.contains("Vehicle 0: 0 -> 1 -> 2 -> 0"), text);
        assertTrue(text.contains("Vehicle 2: 0 -> 4 -> 0"), text);
        assertTrue(text.contains("Best Value: 26.0000"), text);
    }
}
