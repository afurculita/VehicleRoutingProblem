package ro.uaic.info.greedy;

import org.junit.jupiter.api.Test;
import ro.uaic.info.Solution;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VRPLibReader;
import ro.uaic.info.VrpInstance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClarkeWrightSolverTest {

    @Test
    void mergesByDecreasingSavings() {
        Solution solution = new ClarkeWrightSolver(TestInstances.tiny()).solve();
        // s(1,2) = s(1,4) = s(2,3) = s(3,4) = 2 but customer 4 (demand 6) fits with nobody; 1-2 merge first.
        assertEquals(List.of(List.of(1, 2), List.of(3), List.of(4)), solution.getRoutes());
        assertEquals(TestInstances.TINY_OPTIMUM, solution.getCost(), 1e-9);
    }

    @Test
    void beatsNearestNeighbourOnRealInstances() throws IOException {
        for (VrpInstance instance : List.of(TestInstances.pn16k8(),
                VRPLibReader.read(Path.of("datasets", "small", "A-n32-k5.vrp")),
                VRPLibReader.read(Path.of("datasets", "small", "B-n78-k10.vrp")))) {
            double greedy = new GreedySolver(instance).solve().getCost();
            Solution savings = new ClarkeWrightSolver(instance).solve();
            assertTrue(savings.getCost() < greedy, instance.getName() + ": savings " + savings.getCost() + " vs greedy " + greedy);
            assertEquals(Solution.cost(instance, savings.getRoutes()), savings.getCost(), 1e-9);
        }
    }

    @Test
    void isDeterministic() {
        VrpInstance instance = TestInstances.pn16k8();
        assertEquals(new ClarkeWrightSolver(instance).solve().getRoutes(), new ClarkeWrightSolver(instance).solve().getRoutes());
    }
}
