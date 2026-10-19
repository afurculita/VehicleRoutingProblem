package ro.uaic.info;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import ro.uaic.info.acs.AcsParameters;
import ro.uaic.info.acs.AntColonySystemSolver;
import ro.uaic.info.greedy.GreedySolver;
import ro.uaic.info.tabu.TabuSearchSolver;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/**
 * Command line entry point.
 *
 * <pre>
 * java -jar target/vrp-2.0.0.jar -alg tabu -i datasets/small/A-n32-k5.vrp --tabu 5 --iterations 10000 --seed 1
 * java -jar target/vrp-2.0.0.jar -alg acs  -i datasets/small/A-n32-k5.vrp --beta 2 --rho 0.1 --omega 0.1 --q0 0.9 --iterations 100
 * </pre>
 *
 * Prints the routes followed by a {@code Best Value: <cost>} line (consumed by the irace scripts).
 */
public final class VRPRunner {

    static final int DEFAULT_TABU_ITERATIONS = 10_000;
    static final int DEFAULT_ACS_ITERATIONS = 100;

    @Parameter(names = {"--algorithm", "-alg"}, required = true,
            description = "Algorithm to run: greedy, tabu or acs")
    String algorithm;

    @Parameter(names = {"--instance", "-i"}, required = true,
            description = "Path to a TSPLIB/CVRPLIB .vrp instance")
    String instance;

    @Parameter(names = "--round-distances",
            description = "Round Euclidean distances to the nearest integer (TSPLIB EUC_2D convention)")
    boolean roundDistances = false;

    @Parameter(names = "--seed",
            description = "Random seed; a random one is drawn and printed when omitted")
    Long seed;

    @Parameter(names = "--iterations",
            description = "ACS: number of colony iterations (default 100). "
                    + "Tabu: iterations without improvement before stopping (default 10000)")
    Integer iterations;

    // Tabu Search
    @Parameter(names = "--tabu", description = "Tabu: base tabu tenure in iterations; short tenures (5-10) work best")
    int tabuHorizon = 5;

    // Ant Colony System
    @Parameter(names = "--alpha", description = "ACS: pheromone exponent")
    double alpha = 1.0;
    @Parameter(names = "--beta", description = "ACS: visibility (1/distance) exponent")
    double beta = 2.0;
    @Parameter(names = "--rho", description = "ACS: global pheromone evaporation rate, in (0, 1)")
    double rho = 0.1;
    @Parameter(names = "--omega", description = "ACS: local pheromone update rate, in [0, 1]")
    double omega = 0.1;
    @Parameter(names = "--q0", description = "ACS: exploitation probability, in [0, 1]")
    double q0 = 0.9;
    @Parameter(names = "--ants", description = "ACS: ants per iteration (0 = one per customer)")
    int ants = 0;
    @Parameter(names = "--local-search", arity = 1, description = "ACS: improve each ant's routes with 2-opt (true/false)")
    boolean localSearch = true;

    @Parameter(names = {"--help", "-h"}, help = true, description = "Show this help")
    boolean help;

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Parses {@code args}, runs the selected algorithm and prints the result on {@code out}.
     *
     * @return the process exit code: 0 on success, 2 on invalid arguments, 1 on any other error
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        VRPRunner options = new VRPRunner();
        JCommander commander = JCommander.newBuilder()
                .addObject(options)
                .programName("vrp")
                .build();
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            err.println(e.getMessage());
            err.print(usage(commander));
            return 2;
        }
        if (options.help) {
            out.print(usage(commander));
            return 0;
        }
        try {
            Solution solution = options.execute(out);
            out.print(solution.format());
            return 0;
        } catch (NoSuchFileException e) {
            err.println("Error: instance file not found: " + e.getFile());
            return 1;
        } catch (IllegalArgumentException | IOException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static String usage(JCommander commander) {
        StringBuilder sb = new StringBuilder();
        commander.getUsageFormatter().usage(sb);
        return sb.toString();
    }

    /** Loads the instance, builds the solver and runs it, reporting progress on {@code out}. */
    Solution execute(PrintStream out) throws IOException {
        VrpInstance problem = VRPLibReader.read(Path.of(instance), roundDistances);
        long effectiveSeed = seed != null ? seed : new Random().nextLong();
        Solver solver = createSolver(problem, new Random(effectiveSeed));

        out.println("Instance: " + problem.getName() + " (" + problem.getNumberOfCustomers() + " customers, capacity "
                + problem.getCapacity() + ", at least " + problem.getMinimumNumberOfVehicles() + " vehicles)");
        if (problem.hasMaxRouteLength()) {
            out.println("Warning: the instance limits route length to " + problem.getMaxRouteLength()
                    + ", which the solvers do not enforce");
        }
        out.println("Algorithm: " + algorithm.toLowerCase(Locale.ROOT));
        out.println("Seed: " + effectiveSeed);

        long start = System.nanoTime();
        Solution solution = solver.solve();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        out.println("Execution Time: " + elapsedMillis + " ms");
        out.println("=========================================================");
        return solution;
    }

    private int iterationsOr(int defaultValue) {
        return iterations != null ? iterations : defaultValue;
    }

    Solver createSolver(VrpInstance problem, Random random) {
        return switch (algorithm.toLowerCase(Locale.ROOT)) {
            case "greedy" -> new GreedySolver(problem);
            case "tabu" -> new TabuSearchSolver(problem, tabuHorizon, iterationsOr(DEFAULT_TABU_ITERATIONS), random);
            case "acs" -> new AntColonySystemSolver(problem,
                    new AcsParameters(alpha, beta, rho, omega, q0, iterationsOr(DEFAULT_ACS_ITERATIONS), ants, localSearch),
                    random);
            default -> throw new IllegalArgumentException("Unknown algorithm '" + algorithm + "', expected greedy, tabu or acs");
        };
    }
}
