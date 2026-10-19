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
        assertTrue(run.out().contains("Best Value: 499.4408"), run.out());
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
                "--local-search", "false", "--seed", "2");
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Algorithm: acs"), run.out());
        assertTrue(run.out().contains("Best Value: "), run.out());
    }

    @Test
    void acceptsRoundedDistances() {
        Run run = run("-alg", "greedy", "-i", INSTANCE, "--round-distances");
        assertEquals(0, run.exitCode(), run.err());
        // with integer distances the cost is an integer too
        assertTrue(run.out().matches("(?s).*Best Value: \\d+\\.0000.*"), run.out());
    }

    @Test
    void warnsAboutUnenforcedRouteLengthLimit() {
        Run run = run("-alg", "greedy", "-i", Path.of("datasets", "big", "Li_21.vrp").toString());
        assertEquals(0, run.exitCode(), run.err());
        assertTrue(run.out().contains("Warning: the instance limits route length to 1800.0"), run.out());
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
    void unknownAlgorithmExitsWithOne() {
        Run run = run("-alg", "simulated-annealing", "-i", INSTANCE);
        assertEquals(1, run.exitCode());
        assertTrue(run.err().contains("Unknown algorithm"), run.err());
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
