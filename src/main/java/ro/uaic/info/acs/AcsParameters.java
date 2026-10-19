package ro.uaic.info.acs;

/**
 * Parameters of the {@link AntColonySystemSolver}.
 *
 * @param alpha        exponent of the pheromone trail τ in the transition rule (>= 0)
 * @param beta         exponent of the heuristic visibility η = 1/d in the transition rule (>= 0)
 * @param rho          global pheromone evaporation / deposit rate, in (0, 1)
 * @param omega        local pheromone update rate applied to every traversed arc, in [0, 1]
 * @param q0           probability of choosing the greedy (exploitation) step, in [0, 1]
 * @param iterations   number of colony iterations (>= 1)
 * @param numberOfAnts ants per iteration (Dorigo &amp; Gambardella use 10); {@code 0} means "one ant per customer"
 * @param localSearch  how each ant's routes are improved before the pheromone update
 */
public record AcsParameters(double alpha, double beta, double rho, double omega, double q0, int iterations, int numberOfAnts,
                            LocalSearchMode localSearch) {

    /** Local search applied to every ant's solution. */
    public enum LocalSearchMode {
        /** No local search: the raw constructed routes. */
        NONE,
        /** Intra-route 2-opt only (cheap). */
        TWO_OPT,
        /** Full descent over relocate, Or-opt, swap, 2-opt and cross-exchange (strong, slower). */
        FULL
    }

    /** Dorigo &amp; Gambardella's classic ACS settings: 10 ants, a 100-iteration budget and full local search. */
    public static final AcsParameters DEFAULT = new AcsParameters(1.0, 2.0, 0.1, 0.1, 0.9, 100, 10, LocalSearchMode.FULL);

    public AcsParameters {
        requireRange("alpha", alpha, 0.0, Double.POSITIVE_INFINITY);
        requireRange("beta", beta, 0.0, Double.POSITIVE_INFINITY);
        if (!(rho > 0.0 && rho < 1.0)) {
            throw new IllegalArgumentException("rho must be in (0, 1), got " + rho);
        }
        requireRange("omega", omega, 0.0, 1.0);
        requireRange("q0", q0, 0.0, 1.0);
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be >= 1, got " + iterations);
        }
        if (numberOfAnts < 0) {
            throw new IllegalArgumentException("numberOfAnts must be >= 0, got " + numberOfAnts);
        }
        if (localSearch == null) {
            throw new IllegalArgumentException("localSearch must not be null");
        }
    }

    public AcsParameters withIterations(int newIterations) {
        return new AcsParameters(alpha, beta, rho, omega, q0, newIterations, numberOfAnts, localSearch);
    }

    private static void requireRange(String name, double value, double min, double max) {
        if (Double.isNaN(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "], got " + value);
        }
    }
}
