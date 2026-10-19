package ro.uaic.info.local;

import org.junit.jupiter.api.Test;
import ro.uaic.info.Solution;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VRPLibReader;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSearchTest {

    @Test
    void improvesTheGreedySolutionWithCompleteAndGranularNeighbourhoods() throws IOException {
        VrpInstance instance = VRPLibReader.read(Path.of("datasets", "small", "A-n32-k5.vrp"));
        Solution greedy = new GreedySolver(instance).solve();

        for (int k : new int[] {0, 5, 20}) {
            LocalSearch search = new LocalSearch(instance, new CandidateLists(instance, k));
            RouteState state = new RouteState(instance, greedy);
            double gain = search.descend(state);
            Solution improved = state.toSolution();

            assertEquals(greedy.getCost() - improved.getCost(), gain, 1e-6);
            assertTrue(improved.getCost() < greedy.getCost() * 0.9, "k=" + k + ": " + improved.getCost());
            assertEquals(0.0, search.descend(state), 1e-12, "a second descent must find nothing");
        }
    }

    @Test
    void improveDoesNotModifyItsInput() {
        VrpInstance instance = TestInstances.pn16k8();
        List<List<Integer>> input = new GreedySolver(instance).solve().getRoutes();
        List<List<Integer>> output = new LocalSearch(instance, new CandidateLists(instance, 0)).improve(instance, input);
        assertEquals(new GreedySolver(instance).solve().getRoutes(), input);
        assertTrue(Solution.cost(instance, output) <= Solution.cost(instance, input));
    }

    @Test
    void findsTheOptimumOfTheTinyInstanceFromAPoorStart() {
        VrpInstance tiny = TestInstances.tiny();
        List<List<Integer>> poor = List.of(List.of(1), List.of(2), List.of(3), List.of(4));
        List<List<Integer>> improved = new LocalSearch(tiny, new CandidateLists(tiny, 0)).improve(tiny, poor);
        assertEquals(TestInstances.TINY_OPTIMUM, Solution.cost(tiny, improved), 1e-9);
    }
}
