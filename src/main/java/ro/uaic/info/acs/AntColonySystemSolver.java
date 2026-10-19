package ro.uaic.info.acs;

import ro.uaic.info.Solution;
import ro.uaic.info.Solver;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;
import ro.uaic.info.local.CandidateLists;
import ro.uaic.info.local.LocalSearch;
import ro.uaic.info.local.RouteState;
import ro.uaic.info.local.TwoOpt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ant Colony System (Dorigo &amp; Gambardella, 1997) adapted to the CVRP.
 *
 * <p>Each ant builds a complete solution by travelling from the depot and repeatedly choosing the
 * next customer among the unserved ones that still fit in the vehicle (capacity and, if declared,
 * maximum route length). When nothing fits the ant returns to the depot and starts a new route. The
 * choice follows the pseudo-random proportional rule: with probability {@code q0} the ant greedily
 * takes the arc maximising τ<sup>α</sup>·η<sup>β</sup> (η = 1/d), otherwise it draws the next
 * customer with probability proportional to that product. As usual in ACS, the choice is first
 * restricted to the ant's {@link CandidateLists candidate list}; all feasible customers are
 * considered only when no candidate is feasible.
 *
 * <p>Pheromones are initialised to τ<sub>0</sub> = 1 / (n · C<sub>nn</sub>), where C<sub>nn</sub> is
 * the cost of the nearest-neighbour ({@link GreedySolver}) solution. Every arc an ant traverses is
 * immediately updated locally, τ ← (1 − ω)·τ + ω·τ<sub>0</sub>, which makes already-used arcs less
 * attractive to the following ants and so diversifies the search. After each iteration the global
 * update τ ← (1 − ρ)·τ + ρ / L<sub>best</sub> is applied to the arcs of the best-so-far solution.
 *
 * <p>As recommended by Dorigo &amp; Gambardella, each ant's routes are improved by a local search
 * before they compete for the global update: either 2-opt only or the full
 * {@link LocalSearch} descent (see {@link AcsParameters.LocalSearchMode}).
 *
 * <p>Ant {@code k} is forced to visit customer {@code 1 + (k mod numberOfCustomers)} first, so with
 * one ant per customer every customer starts a tour ("one ant at each vertex"); with the default of
 * 10 ants the starting customers rotate across iterations. Runs are reproducible for a given
 * {@link Random} seed.
 */
public final class AntColonySystemSolver implements Solver {

    /** Distances below this value are clamped when computing η = 1/d (coincident customers). */
    private static final double MIN_DISTANCE = 1e-6;

    private final VrpInstance instance;
    private final AcsParameters params;
    private final Random random;
    private final CandidateLists candidates;
    private final LocalSearch localSearch;

    private final int n;
    private final int numberOfAnts;
    private final double[][] tau;
    /** η<sup>β</sup>, precomputed. */
    private final double[][] etaBeta;
    private final double tau0;

    public AntColonySystemSolver(VrpInstance instance, AcsParameters params, Random random) {
        this(instance, params, random, new CandidateLists(instance, 0));
    }

    public AntColonySystemSolver(VrpInstance instance, AcsParameters params, Random random, CandidateLists candidates) {
        this.instance = instance;
        this.params = params;
        this.random = random;
        this.candidates = candidates;
        this.localSearch = params.localSearch() == AcsParameters.LocalSearchMode.FULL ? new LocalSearch(instance, candidates) : null;
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
                int firstCustomer = 1 + (it * numberOfAnts + k) % instance.getNumberOfCustomers();
                List<List<Integer>> routes = constructSolution(firstCustomer);
                routes = improve(routes);
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

    private List<List<Integer>> improve(List<List<Integer>> routes) {
        return switch (params.localSearch()) {
            case NONE -> routes;
            case TWO_OPT -> {
                TwoOpt.improveAll(instance, routes);
                yield routes;
            }
            case FULL -> {
                RouteState state = new RouteState(instance, routes);
                localSearch.descend(state);
                yield state.toCustomerRoutes();
            }
        };
    }

    /** One ant's walk: a complete feasible set of routes, starting with {@code firstCustomer}. */
    private List<List<Integer>> constructSolution(int firstCustomer) {
        boolean[] served = new boolean[n];
        int remaining = instance.getNumberOfCustomers();
        List<List<Integer>> routes = new ArrayList<>();
        List<Integer> route = new ArrayList<>();
        int current = VrpInstance.DEPOT;
        int load = 0;
        double length = 0.0;

        int[] feasible = new int[n];
        double[] weights = new double[n];

        while (remaining > 0) {
            int next;
            if (current == VrpInstance.DEPOT && route.isEmpty() && !served[firstCustomer]) {
                next = firstCustomer;
            } else {
                int count = collectCandidates(current, served, load, length, candidates.of(current), feasible);
                if (count == 0 && !candidates.isComplete()) {
                    count = collectAll(current, served, load, length, feasible);
                }
                if (count == 0) {
                    // Vehicle is full: close the route at the depot and start a new one.
                    localUpdate(current, VrpInstance.DEPOT);
                    routes.add(route);
                    route = new ArrayList<>();
                    current = VrpInstance.DEPOT;
                    load = 0;
                    length = 0.0;
                    continue;
                }
                next = chooseNext(current, feasible, count, weights);
            }
            localUpdate(current, next);
            route.add(next);
            served[next] = true;
            load += instance.getDemand(next);
            length += instance.getDistance(current, next);
            remaining--;
            current = next;
        }
        localUpdate(current, VrpInstance.DEPOT);
        routes.add(route);
        return routes;
    }

    private boolean feasible(int current, int c, boolean[] served, int load, double length) {
        return !served[c] && c != VrpInstance.DEPOT && instance.fits(load, c)
                && length + instance.getDistance(current, c) + instance.getDistance(c, VrpInstance.DEPOT) <= instance.getMaxRouteLength();
    }

    private int collectCandidates(int current, boolean[] served, int load, double length, int[] from, int[] out) {
        int count = 0;
        for (int c : from) {
            if (feasible(current, c, served, load, length)) {
                out[count++] = c;
            }
        }
        return count;
    }

    private int collectAll(int current, boolean[] served, int load, double length, int[] out) {
        int count = 0;
        for (int c = 1; c < n; c++) {
            if (feasible(current, c, served, load, length)) {
                out[count++] = c;
            }
        }
        return count;
    }

    /** Pseudo-random proportional rule. */
    private int chooseNext(int current, int[] feasible, int count, double[] weights) {
        double total = 0.0;
        int argmax = feasible[0];
        double max = -1.0;
        for (int k = 0; k < count; k++) {
            int c = feasible[k];
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
                return feasible[k];
            }
        }
        return feasible[count - 1]; // rounding guard
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
