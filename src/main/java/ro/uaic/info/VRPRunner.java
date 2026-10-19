package ro.uaic.info;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import ro.uaic.info.VRPLibReader.DistanceMode;
import ro.uaic.info.acs.AcsParameters;
import ro.uaic.info.acs.AcsParameters.LocalSearchMode;
import ro.uaic.info.acs.AntColonySystemSolver;
import ro.uaic.info.greedy.ClarkeWrightSolver;
import ro.uaic.info.greedy.GreedySolver;
import ro.uaic.info.local.CandidateLists;
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
 * java -jar target/vrp-2.0.0.jar -alg tabu -i datasets/small/A-n32-k5.vrp --seed 1
 * java -jar target/vrp-2.0.0.jar -alg acs  -i datasets/small/A-n32-k5.vrp --seed 1 --iterations 50
 * java -jar target/vrp-2.0.0.jar -alg hybrid -i datasets/big/X-n101-k25.vrp --seed 1
 * </pre>
 *
 * Prints the routes followed by a {@code Best Value: <cost>} line (consumed by the irace scripts).
 */
public final class VRPRunner {

    static final int DEFAULT_TABU_ITERATIONS = 10_000;
    static final int DEFAULT_ACS_ITERATIONS = 100;

    /** Algorithms selectable with {@code -alg}. */
    public enum Algorithm {
        /** Nearest-neighbour construction. */
        GREEDY,
        /** Clarke &amp; Wright savings construction. */
        SAVINGS,
        /** Tabu search started from the savings solution. */
        TABU,
        /** Ant colony system. */
        ACS,
        /** Ant colony system followed by a tabu search from its best solution. */
        HYBRID
    }

    /** Initial solution of the tabu search. */
    public enum Init {
        GREEDY, SAVINGS
    }

    @Parameter(names = {"--algorithm", "-alg"}, required = true,
            description = "Algorithm to run: greedy, savings, tabu, acs or hybrid (acs then tabu)")
    Algorithm algorithm;

    @Parameter(names = {"--instance", "-i"}, required = true,
            description = "Path to a TSPLIB/CVRPLIB .vrp instance")
    String instance;

    @Parameter(names = "--distances",
            description = "Distance evaluation: auto (rounded for A/B/P/M/X, exact for Golden/Li), exact or rounded")
    DistanceMode distances = DistanceMode.AUTO;

    @Parameter(names = "--seed",
            description = "Random seed; a random one is drawn and printed when omitted")
    Long seed;

    @Parameter(names = "--iterations",
            description = "ACS: number of colony iterations (default 100). "
                    + "Tabu and hybrid: iterations without improvement before stopping (default 10000)")
    Integer iterations;

    @Parameter(names = "--granularity",
            description = "Size of the candidate lists restricting the neighbourhood (0 = complete neighbourhood)")
    int granularity = 30;

    // Tabu Search
    @Parameter(names = "--tabu", description = "Tabu: base tabu tenure in iterations; short tenures (5-10) work best")
    int tabuHorizon = 5;
    @Parameter(names = "--penalty", description = "Tabu: frequency penalty factor of the long-term memory (0 = off)")
    double penalty = 0.1;
    @Parameter(names = "--init", description = "Tabu: initial solution, greedy or savings")
    Init init = Init.SAVINGS;

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
    int ants = 10;
    @Parameter(names = "--local-search", description = "ACS: local search applied to each ant: none, two_opt or full")
    LocalSearchMode localSearch = LocalSearchMode.FULL;
    @Parameter(names = "--acs-iterations", description = "Hybrid: colony iterations of the ACS phase")
    int acsIterations = DEFAULT_ACS_ITERATIONS;

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
        VrpInstance problem = VRPLibReader.read(Path.of(instance), distances);
        long effectiveSeed = seed != null ? seed : new Random().nextLong();
        Solver solver = createSolver(problem, new Random(effectiveSeed));

        out.println("Instance: " + problem.getName() + " (" + problem.getNumberOfCustomers() + " customers, capacity "
                + problem.getCapacity() + ", at least " + problem.getMinimumNumberOfVehicles() + " vehicles"
                + (problem.hasMaxRouteLength() ? ", maximum route length " + problem.getMaxRouteLength() : "") + ")");
        out.println("Algorithm: " + algorithm.name().toLowerCase(Locale.ROOT));
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
        CandidateLists candidates = new CandidateLists(problem, granularity);
        return switch (algorithm) {
            case GREEDY -> new GreedySolver(problem);
            case SAVINGS -> new ClarkeWrightSolver(problem);
            case TABU -> () -> tabu(problem, random, candidates).solveFrom(initialSolution(problem));
            case ACS -> new AntColonySystemSolver(problem, acsParameters(iterationsOr(DEFAULT_ACS_ITERATIONS)), random, candidates);
            case HYBRID -> () -> {
                Solution colony = new AntColonySystemSolver(problem, acsParameters(acsIterations), random, candidates).solve();
                return tabu(problem, random, candidates).solveFrom(colony);
            };
        };
    }

    private Solution initialSolution(VrpInstance problem) {
        return init == Init.GREEDY ? new GreedySolver(problem).solve() : new ClarkeWrightSolver(problem).solve();
    }

    private TabuSearchSolver tabu(VrpInstance problem, Random random, CandidateLists candidates) {
        return new TabuSearchSolver(problem, tabuHorizon, iterationsOr(DEFAULT_TABU_ITERATIONS), penalty, random, candidates);
    }

    private AcsParameters acsParameters(int colonyIterations) {
        return new AcsParameters(alpha, beta, rho, omega, q0, colonyIterations, ants, localSearch);
    }
}
