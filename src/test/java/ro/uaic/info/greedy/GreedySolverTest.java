package ro.uaic.info.greedy;

import org.junit.jupiter.api.Test;
import ro.uaic.info.Solution;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VrpInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreedySolverTest {

    @Test
    void visitsNearestFeasibleCustomerFirst() {
        Solution solution = new GreedySolver(TestInstances.tiny()).solve();

        // From the depot customers 1 and 3 are both at distance 3; the lowest index wins. Customer 2 is
        // then the closest one that still fits (customer 4 has demand 6 and would exceed the capacity).
        assertEquals(List.of(List.of(1, 2), List.of(3), List.of(4)), solution.getRoutes());
        assertEquals(TestInstances.TINY_OPTIMUM, solution.getCost(), 1e-12);
    }

    @Test
    void producesFeasibleSolutionOnRealInstance() {
        VrpInstance instance = TestInstances.pn16k8();
        Solution solution = new GreedySolver(instance).solve();

        // Solution.of already validates feasibility; check the reported cost is consistent.
        assertEquals(Solution.cost(instance, solution.getRoutes()), solution.getCost(), 1e-9);
        assertEquals(8, solution.getNumberOfRoutes());
        assertEquals(497.0, solution.getCost(), 1e-9);
    }

    @Test
    void isDeterministic() {
        VrpInstance instance = TestInstances.pn16k8();
        assertEquals(new GreedySolver(instance).solve().getRoutes(), new GreedySolver(instance).solve().getRoutes());
    }
}
