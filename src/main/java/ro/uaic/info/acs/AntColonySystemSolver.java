package ro.uaic.info.acs;

import ro.uaic.info.Solution;
import ro.uaic.info.Solver;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;
import ro.uaic.info.local.TwoOpt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ant Colony System (Dorigo &amp; Gambardella, 1997) adapted to the CVRP.
 *
 * <p>Each ant builds a complete solution by travelling from the depot and repeatedly choosing the
 * next customer among the unserved ones that still fit in the vehicle. When nothing fits the ant
 * returns to the depot and starts a new route. The choice follows the pseudo-random proportional
 * rule: with probability {@code q0} the ant greedily takes the arc maximising
 * τ<sup>α</sup>·η<sup>β</sup> (η = 1/d), otherwise it draws the next customer with probability
 * proportional to that product.
 *
 * <p>Pheromones are initialised to τ<sub>0</sub> = 1 / (n · C<sub>nn</sub>), where C<sub>nn</sub> is
 * the cost of the nearest-neighbour ({@link GreedySolver}) solution. Every arc an ant traverses is
 * immediately updated locally, τ ← (1 − ω)·τ + ω·τ<sub>0</sub>, which makes already-used arcs less
 * attractive to the following ants and so diversifies the search. After each iteration the global
 * update τ ← (1 − ρ)·τ + ρ / L<sub>best</sub> is applied to the arcs of the best-so-far solution.
 *
 * <p>As recommended by Dorigo &amp; Gambardella, each ant's routes are optionally improved with a
 * 2-opt local search ({@link TwoOpt}) before they compete for the global update.
 *
 * <p>To mimic "one ant at each customer", ant {@code k} is forced to visit customer
 * {@code 1 + (k mod numberOfCustomers)} first. Runs are reproducible for a given {@link Random} seed.
 */
public final class AntColonySystemSolver implements Solver {

    /** Distances below this value are clamped when computing η = 1/d (coincident customers). */
    private static final double MIN_DISTANCE = 1e-6;

    private final VrpInstance instance;
    private final AcsParameters params;
    private final Random random;

    private final int n;
    private final int numberOfAnts;
    private final double[][] tau;
    /** η<sup>β</sup>, precomputed. */
    private final double[][] etaBeta;
    private final double tau0;

    public AntColonySystemSolver(VrpInstance instance, AcsParameters params, Random random) {
        this.instance = instance;
        this.params = params;
        this.random = random;
        this.n = instance.getDimension();
        this.numberOfAnts = params.numberOfAnts() == 0 ? instance.getNumberOfCustomers() : params.numberOfAnts();

        double cnn = new GreedySolver(instance).solve().getCost();
        this.tau0 = 1.0 / (n * Math.max(cnn, MIN_DISTANCE));

        this.tau = new double[n][n];
        this.etaBeta = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tau[i][j] = tau0;
                if (i != j) {
                    etaBeta[i][j] = Math.pow(1.0 / Math.max(instance.getDistance(i, j), MIN_DISTANCE), params.beta());
                }
            }
        }
    }

    /** Initial pheromone level τ<sub>0</sub>. */
    public double getTau0() {
        return tau0;
    }

    public int getNumberOfAnts() {
        return numberOfAnts;
    }

    @Override
    public Solution solve() {
        List<List<Integer>> bestRoutes = null;
        double bestCost = Double.POSITIVE_INFINITY;

        for (int it = 0; it < params.iterations(); it++) {
            for (int k = 0; k < numberOfAnts; k++) {
                int firstCustomer = 1 + k % instance.getNumberOfCustomers();
                List<List<Integer>> routes = constructSolution(firstCustomer);
                if (params.localSearch()) {
                    TwoOpt.improveAll(instance, routes);
                }
                double cost = Solution.cost(instance, routes);
                if (cost < bestCost - Solution.EPSILON) {
                    bestCost = cost;
                    bestRoutes = routes;
                }
            }
            globalUpdate(bestRoutes, bestCost);
        }
        return Solution.of(instance, bestRoutes);
    }

    /** One ant's walk: a complete feasible set of routes, starting with {@code firstCustomer}. */
    private List<List<Integer>> constructSolution(int firstCustomer) {
        boolean[] served = new boolean[n];
        int remaining = instance.getNumberOfCustomers();
        List<List<Integer>> routes = new ArrayList<>();
        List<Integer> route = new ArrayList<>();
        int current = VrpInstance.DEPOT;
        int load = 0;

        int[] candidates = new int[n];
        double[] weights = new double[n];

        while (remaining > 0) {
            int next;
            if (current == VrpInstance.DEPOT && route.isEmpty() && !served[firstCustomer]) {
                next = firstCustomer;
            } else {
                int count = 0;
                for (int c = 1; c < n; c++) {
                    if (!served[c] && load + instance.getDemand(c) <= instance.getCapacity()) {
                        candidates[count++] = c;
                    }
                }
                if (count == 0) {
                    // Vehicle is full: close the route at the depot and start a new one.
                    localUpdate(current, VrpInstance.DEPOT);
                    routes.add(route);
                    route = new ArrayList<>();
                    current = VrpInstance.DEPOT;
                    load = 0;
                    continue;
                }
                next = chooseNext(current, candidates, count, weights);
            }
            localUpdate(current, next);
            route.add(next);
            served[next] = true;
            load += instance.getDemand(next);
            remaining--;
            current = next;
        }
        localUpdate(current, VrpInstance.DEPOT);
        routes.add(route);
        return routes;
    }

    /** Pseudo-random proportional rule. */
    private int chooseNext(int current, int[] candidates, int count, double[] weights) {
        double total = 0.0;
        int argmax = candidates[0];
        double max = -1.0;
        for (int k = 0; k < count; k++) {
            int c = candidates[k];
            double w = attractiveness(current, c);
            weights[k] = w;
            total += w;
            if (w > max) {
                max = w;
                argmax = c;
            }
        }
        if (random.nextDouble() < params.q0() || total <= 0.0 || Double.isInfinite(total)) {
            return argmax; // exploitation
        }
        double r = random.nextDouble() * total; // biased exploration (roulette wheel)
        double acc = 0.0;
        for (int k = 0; k < count; k++) {
            acc += weights[k];
            if (r < acc) {
                return candidates[k];
            }
        }
        return candidates[count - 1]; // rounding guard
    }

    private double attractiveness(int i, int j) {
        double t = params.alpha() == 1.0 ? tau[i][j] : Math.pow(tau[i][j], params.alpha());
        return t * etaBeta[i][j];
    }

    private void localUpdate(int i, int j) {
        if (i == j) {
            return;
        }
        double value = (1.0 - params.omega()) * tau[i][j] + params.omega() * tau0;
        tau[i][j] = value;
        tau[j][i] = value;
    }

    private void globalUpdate(List<List<Integer>> bestRoutes, double bestCost) {
        double keep = 1.0 - params.rho();
        double deposit = params.rho() / bestCost;
        for (List<Integer> route : bestRoutes) {
            int prev = VrpInstance.DEPOT;
            for (int k = 0; k <= route.size(); k++) {
                int node = k < route.size() ? route.get(k) : VrpInstance.DEPOT;
                double value = keep * tau[prev][node] + deposit;
                tau[prev][node] = value;
                tau[node][prev] = value;
                prev = node;
            }
        }
    }
}
