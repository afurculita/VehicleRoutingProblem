package ro.uaic.info;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VRPRunnerTest {

    private record Run(int exitCode, String out, String err) {
    }

    private static Run run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = VRPRunner.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static final String INSTANCE = TestInstances.P_N16_K8.toString();

    @Test
    void runsGreedy() {
        Run run = run("-alg", "greedy", "-i", INSTANCE);
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Instance: P-n16-k8 (15 customers, capacity 35, at least 8 vehicles)"), run.out());
        assertTrue(run.out().contains("Algorithm: greedy"), run.out());
        assertTrue(run.out().contains("Best Value: 497.0000"), run.out()); // rounded distances by default
    }

    @Test
    void runsSavings() {
        Run run = run("-alg", "savings", "-i", INSTANCE);
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Algorithm: savings"), run.out());
        assertTrue(run.out().contains("Best Value: "), run.out());
    }

    @Test
    void runsHybrid() {
        Run run = run("-alg", "hybrid", "-i", INSTANCE, "--acs-iterations", "3", "--iterations", "50", "--seed", "4");
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Algorithm: hybrid"), run.out());
        assertTrue(run.out().contains("Best Value: "), run.out());
    }

    @Test
    void acceptsNeighbourhoodAndInitialSolutionOptions() {
        Run run = run("-alg", "tabu", "-i", INSTANCE, "--iterations", "50", "--granularity", "0", "--init", "greedy",
                "--penalty", "0", "--seed", "1");
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Best Value: "), run.out());
    }

    @Test
    void rejectsUnknownEnumValues() {
        assertEquals(2, run("-alg", "tabu", "-i", INSTANCE, "--init", "random").exitCode());
        assertEquals(2, run("-alg", "acs", "-i", INSTANCE, "--local-search", "sometimes").exitCode());
    }

    @Test
    void runsTabuWithExplicitParameters() {
        Run run = run("--algorithm", "tabu", "--instance", INSTANCE, "--tabu", "5", "--iterations", "200", "--seed", "1");
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Seed: 1"), run.out());
        assertTrue(run.out().contains("Best Value: "), run.out());
        assertTrue(run.out().contains("Execution Time: "), run.out());
    }

    @Test
    void runsAcsWithExplicitParameters() {
        Run run = run("-alg", "acs", "-i", INSTANCE, "--iterations", "5", "--ants", "4", "--q0", "0.5",
                "--local-search", "none", "--seed", "2");
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Algorithm: acs"), run.out());
        assertTrue(run.out().contains("Best Value: "), run.out());
    }

    @Test
    void acceptsRoundedDistances() {
        Run rounded = run("-alg", "greedy", "-i", INSTANCE, "--distances", "rounded");
        assertEquals(0, rounded.exitCode(), rounded.err());
        assertTrue(rounded.out().contains("Best Value: 497.0000"), rounded.out());

        Run exact = run("-alg", "greedy", "-i", INSTANCE, "--distances", "EXACT");
        assertEquals(0, exact.exitCode(), exact.err());
        assertTrue(exact.out().contains("Best Value: 499.4408"), exact.out());
    }

    @Test
    void reportsRouteLengthLimit() {
        Run run = run("-alg", "greedy", "-i", Path.of("datasets", "big", "Li_21.vrp").toString());
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("maximum route length 1800.0"), run.out());
    }

    @Test
    void algorithmNameIsCaseInsensitive() {
        assertEquals(0, run("-alg", "GREEDY", "-i", INSTANCE).exitCode());
    }

    @Test
    void sameSeedGivesSameOutput() {
        Run a = run("-alg", "tabu", "-i", INSTANCE, "--iterations", "100", "--seed", "5");
        Run b = run("-alg", "tabu", "-i", INSTANCE, "--iterations", "100", "--seed", "5");
        assertEquals(bestValue(a.out()), bestValue(b.out()));
    }

    private static String bestValue(String out) {
        return out.lines().filter(l -> l.startsWith("Best Value:")).findFirst().orElseThrow();
    }

    @Test
    void helpExitsWithZero() {
        Run run = run("--help");
        assertEquals(0, run.exitCode());
    }

    @Test
    void missingRequiredOptionsExitWithTwo() {
        Run run = run("-i", INSTANCE);
        assertEquals(2, run.exitCode());
        assertTrue(run.err().contains("--algorithm"), run.err());
    }

    @Test
    void unknownAlgorithmExitsWithTwo() {
        Run run = run("-alg", "simulated-annealing", "-i", INSTANCE);
        assertEquals(2, run.exitCode());
        assertTrue(run.err().contains("-alg"), run.err());
    }

    @Test
    void missingInstanceFileExitsWithOne() {
        Run run = run("-alg", "greedy", "-i", "datasets/does-not-exist.vrp");
        assertEquals(1, run.exitCode());
        assertTrue(run.err().contains("not found"), run.err());
    }

    @Test
    void invalidParameterValueExitsWithOne() {
        Run run = run("-alg", "acs", "-i", INSTANCE, "--rho", "1.0");
        assertEquals(1, run.exitCode());
        assertTrue(run.err().contains("rho"), run.err());
    }
}
