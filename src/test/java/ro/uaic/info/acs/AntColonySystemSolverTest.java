package ro.uaic.info.acs;

import org.junit.jupiter.api.Test;
import ro.uaic.info.acs.AcsParameters.LocalSearchMode;
import ro.uaic.info.local.CandidateLists;
import ro.uaic.info.Solution;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntColonySystemSolverTest {

    @Test
    void findsTheOptimumOfTheTinyInstance() {
        Solution solution = new AntColonySystemSolver(TestInstances.tiny(), AcsParameters.DEFAULT, new Random(1)).solve();
        assertEquals(TestInstances.TINY_OPTIMUM, solution.getCost(), 1e-9);
    }

    @Test
    void producesFeasibleSolutionNoWorseThanGreedy() {
        VrpInstance instance = TestInstances.pn16k8();
        double greedy = new GreedySolver(instance).solve().getCost();
        AcsParameters params = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.9, 20, 0, LocalSearchMode.FULL);

        Solution solution = new AntColonySystemSolver(instance, params, new Random(11)).solve();

        assertTrue(solution.getCost() <= greedy + 1e-9, "acs " + solution.getCost() + " vs greedy " + greedy);
        assertEquals(Solution.cost(instance, solution.getRoutes()), solution.getCost(), 1e-9);
    }

    @Test
    void worksWithoutLocalSearchAndWithFewAnts() {
        VrpInstance instance = TestInstances.pn16k8();
        AcsParameters params = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.9, 20, 3, LocalSearchMode.NONE);

        AntColonySystemSolver solver = new AntColonySystemSolver(instance, params, new Random(5));

        assertEquals(3, solver.getNumberOfAnts());
        Solution solution = solver.solve();
        assertEquals(Solution.cost(instance, solution.getRoutes()), solution.getCost(), 1e-9);
    }

    @Test
    void zeroAntsMeansOneAntPerCustomer() {
        VrpInstance instance = TestInstances.pn16k8();
        AcsParameters params = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.9, 10, 0, LocalSearchMode.NONE);
        assertEquals(instance.getNumberOfCustomers(), new AntColonySystemSolver(instance, params, new Random(5)).getNumberOfAnts());
        assertEquals(10, new AntColonySystemSolver(instance, AcsParameters.DEFAULT, new Random(5)).getNumberOfAnts());
    }

    @Test
    void initialPheromoneIsInverseOfNodesTimesNearestNeighbourCost() {
        VrpInstance instance = TestInstances.tiny();
        double cnn = new GreedySolver(instance).solve().getCost();

        AntColonySystemSolver solver = new AntColonySystemSolver(instance, AcsParameters.DEFAULT, new Random(5));

        assertEquals(1.0 / (instance.getDimension() * cnn), solver.getTau0(), 1e-15);
    }

    @Test
    void isReproducibleForAGivenSeed() {
        VrpInstance instance = TestInstances.pn16k8();
        AcsParameters params = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.5, 10, 0, LocalSearchMode.TWO_OPT);
        Solution a = new AntColonySystemSolver(instance, params, new Random(99)).solve();
        Solution b = new AntColonySystemSolver(instance, params, new Random(99)).solve();
        assertEquals(a.getRoutes(), b.getRoutes());
    }

    @Test
    void granularCandidateListsStillBuildFeasibleSolutions() {
        VrpInstance instance = TestInstances.pn16k8();
        AcsParameters params = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.9, 10, 0, LocalSearchMode.TWO_OPT);
        Solution solution = new AntColonySystemSolver(instance, params, new Random(3), new CandidateLists(instance, 2)).solve();
        assertEquals(Solution.cost(instance, solution.getRoutes()), solution.getCost(), 1e-9);
    }

    @Test
    void pureExplorationStillBuildsFeasibleSolutions() {
        VrpInstance instance = TestInstances.pn16k8();
        AcsParameters params = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.0, 5, 0, LocalSearchMode.NONE);
        Solution solution = new AntColonySystemSolver(instance, params, new Random(1)).solve();
        assertEquals(Solution.cost(instance, solution.getRoutes()), solution.getCost(), 1e-9);
    }
}
