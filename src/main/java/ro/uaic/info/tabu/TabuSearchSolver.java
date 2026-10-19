package ro.uaic.info.tabu;

import ro.uaic.info.Solution;
import ro.uaic.info.Solver;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.GreedySolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tabu Search for the CVRP.
 *
 * <p>The search starts from the {@link GreedySolver} solution and, at every iteration, scans the
 * complete neighbourhood made of three classic move types:
 * <ul>
 *   <li><b>relocate</b> (1-0 exchange): move one customer to any other position of any route,
 *       including an always-available empty route so that a new vehicle can be opened;</li>
 *   <li><b>swap</b> (1-1 exchange): exchange two customers of two different routes;</li>
 *   <li><b>2-opt</b>: reverse a segment inside one route.</li>
 * </ul>
 * Capacity constraints are always respected. The best admissible move is applied even when it
 * worsens the solution, which is what lets the search leave local optima.
 *
 * <p>Three kinds of tabu attributes are combined (in the spirit of Osman's and Gendreau, Hertz &amp;
 * Laporte's tabu searches for the VRP):
 * <ul>
 *   <li>a customer that has just been moved may not be moved again (all move types);</li>
 *   <li>a customer that has just left a route may not re-enter that route (inter-route relocate
 *       and swap), which stops a customer from oscillating between two routes;</li>
 *   <li>an arc that has just been broken may not be rebuilt (intra-route relocate and 2-opt).</li>
 * </ul>
 * The tenure is {@code tabuHorizon} plus a random offset in {@code [0, 5)}. Because every iteration
 * freezes two or three customers, short tenures (5–10) work best; long ones starve the search. The
 * standard aspiration criterion applies: a tabu move is allowed if it yields a new best solution.
 *
 * <p>Diversification: after every {@value #STAGNATION_BEFORE_PERTURBATION} consecutive non-improving
 * iterations the search restarts from the best solution perturbed by a few random moves, whose
 * attributes are made tabu so the kick cannot be undone right away.
 *
 * <p>The search stops after {@code maxIterationsWithoutImprovement} consecutive iterations that did
 * not improve the best known solution, or when no admissible move exists.
 *
 * <p>Runs are reproducible for a given {@link Random} seed. Each iteration costs O(n²).
 */
public final class TabuSearchSolver implements Solver {

    /** Upper bound (exclusive) of the random offset added to the tabu tenure. */
    static final int TENURE_JITTER = 5;
    /** Number of consecutive non-improving iterations after which the solution is perturbed. */
    static final int STAGNATION_BEFORE_PERTURBATION = 50;
    /** Perturbation strength: {@code max(3, customers / PERTURBATION_DIVISOR)} random moves. */
    static final int PERTURBATION_DIVISOR = 10;

    private final VrpInstance instance;
    private final int tabuHorizon;
    private final int maxIterationsWithoutImprovement;
    private final Random random;

    /**
     * @param instance                        the problem
     * @param tabuHorizon                     base tabu tenure, in iterations (>= 0)
     * @param maxIterationsWithoutImprovement stopping criterion (>= 1)
     * @param random                          source of randomness for the tenure jitter
     */
    public TabuSearchSolver(VrpInstance instance, int tabuHorizon, int maxIterationsWithoutImprovement, Random random) {
        if (tabuHorizon < 0) {
            throw new IllegalArgumentException("tabuHorizon must be >= 0, got " + tabuHorizon);
        }
        if (maxIterationsWithoutImprovement < 1) {
            throw new IllegalArgumentException("maxIterationsWithoutImprovement must be >= 1, got " + maxIterationsWithoutImprovement);
        }
        this.instance = instance;
        this.tabuHorizon = tabuHorizon;
        this.maxIterationsWithoutImprovement = maxIterationsWithoutImprovement;
        this.random = random;
    }

    @Override
    public Solution solve() {
        return solveFrom(new GreedySolver(instance).solve());
    }

    /** Runs the search starting from the given feasible solution. */
    public Solution solveFrom(Solution initial) {
        State state = new State(initial);
        int n = instance.getDimension();
        // Routes are never removed, so a route index identifies a route for the whole search;
        // at most one spare empty route is added per customer, hence n + 1 routes.
        TabuList tabu = new TabuList(n, n + 1);

        List<List<Integer>> bestRoutes = state.copyRoutes();
        double bestCost = state.cost;
        int iteration = 0;
        int stagnation = 0;

        while (stagnation < maxIterationsWithoutImprovement) {
            Move move = bestMove(state, tabu, iteration, bestCost);
            if (move == null) {
                break; // every move is tabu and none aspires: the search is stuck
            }
            iteration++;
            state.apply(move, tabu, iteration);

            if (state.cost < bestCost - Solution.EPSILON) {
                bestCost = state.cost;
                bestRoutes = state.copyRoutes();
                stagnation = 0;
            } else {
                stagnation++;
                if (stagnation % STAGNATION_BEFORE_PERTURBATION == 0 && stagnation < maxIterationsWithoutImprovement) {
                    state.reset(bestRoutes, bestCost);
                    state.perturb(tabu, iteration, Math.max(3, instance.getNumberOfCustomers() / PERTURBATION_DIVISOR));
                }
            }
        }
        return Solution.of(instance, bestRoutes);
    }

    /** Scans the whole neighbourhood and returns the best admissible move, or null if there is none. */
    private Move bestMove(State state, TabuList tabu, int iteration, double bestCost) {
        Scan scan = new Scan(state, tabu, iteration, bestCost);
        scanRelocates(state, scan);
        scanSwaps(state, scan);
        scanTwoOpts(state, scan);
        return scan.best;
    }

    private void scanRelocates(State state, Scan scan) {
        int spareRoute = state.firstEmptyRoute();
        for (int from = 0; from < state.routes.size(); from++) {
            List<Integer> source = state.routes.get(from);
            for (int i = 1; i < source.size() - 1; i++) {
                int customer = source.get(i);
                int prev = source.get(i - 1);
                int next = source.get(i + 1);
                double removalGain = instance.getDistance(prev, customer) + instance.getDistance(customer, next)
                        - instance.getDistance(prev, next);
                int demand = instance.getDemand(customer);

                for (int to = 0; to < state.routes.size(); to++) {
                    List<Integer> target = state.routes.get(to);
                    boolean sameRoute = from == to;
                    if (!sameRoute) {
                        if (state.load[to] + demand > instance.getCapacity()) {
                            continue;
                        }
                        if (target.size() == 2 && to != spareRoute) {
                            continue; // only one empty route is worth considering
                        }
                    }
                    for (int j = 0; j < target.size() - 1; j++) {
                        if (sameRoute && (j == i || j == i - 1)) {
                            continue; // re-inserting at the same place
                        }
                        int a = target.get(j);
                        int b = target.get(j + 1);
                        double delta = instance.getDistance(a, customer) + instance.getDistance(customer, b)
                                - instance.getDistance(a, b) - removalGain;
                        if (!scan.isCandidate(delta)) {
                            continue;
                        }
                        boolean tabu = scan.customerTabu(customer) || (sameRoute
                                ? scan.arcTabu(prev, next) || scan.arcTabu(a, customer) || scan.arcTabu(customer, b)
                                : scan.routeTabu(customer, to));
                        if (scan.admissible(delta, tabu)) {
                            scan.record(new Relocate(from, i, to, j, delta));
                        }
                    }
                }
            }
        }
    }

    private void scanSwaps(State state, Scan scan) {
        for (int r1 = 0; r1 < state.routes.size(); r1++) {
            List<Integer> route1 = state.routes.get(r1);
            for (int r2 = r1 + 1; r2 < state.routes.size(); r2++) {
                List<Integer> route2 = state.routes.get(r2);
                for (int i = 1; i < route1.size() - 1; i++) {
                    int c1 = route1.get(i);
                    int p1 = route1.get(i - 1);
                    int n1 = route1.get(i + 1);
                    int d1 = instance.getDemand(c1);
                    for (int k = 1; k < route2.size() - 1; k++) {
                        int c2 = route2.get(k);
                        int d2 = instance.getDemand(c2);
                        if (state.load[r1] - d1 + d2 > instance.getCapacity()
                                || state.load[r2] - d2 + d1 > instance.getCapacity()) {
                            continue;
                        }
                        int p2 = route2.get(k - 1);
                        int n2 = route2.get(k + 1);
                        double delta = instance.getDistance(p1, c2) + instance.getDistance(c2, n1)
                                - instance.getDistance(p1, c1) - instance.getDistance(c1, n1)
                                + instance.getDistance(p2, c1) + instance.getDistance(c1, n2)
                                - instance.getDistance(p2, c2) - instance.getDistance(c2, n2);
                        if (scan.isCandidate(delta)
                                && scan.admissible(delta, scan.customerTabu(c1) || scan.customerTabu(c2)
                                || scan.routeTabu(c1, r2) || scan.routeTabu(c2, r1))) {
                            scan.record(new Swap(r1, i, r2, k, delta));
                        }
                    }
                }
            }
        }
    }

    private void scanTwoOpts(State state, Scan scan) {
        for (int r = 0; r < state.routes.size(); r++) {
            List<Integer> route = state.routes.get(r);
            for (int i = 1; i < route.size() - 2; i++) {
                int prev = route.get(i - 1);
                int ci = route.get(i);
                for (int j = i + 1; j < route.size() - 1; j++) {
                    if (i == 1 && j == route.size() - 2) {
                        continue; // reversing the whole route is a no-op with symmetric distances
                    }
                    int cj = route.get(j);
                    int next = route.get(j + 1);
                    double delta = instance.getDistance(prev, cj) + instance.getDistance(ci, next)
                            - instance.getDistance(prev, ci) - instance.getDistance(cj, next);
                    if (scan.isCandidate(delta)
                            && scan.admissible(delta, scan.customerTabu(ci) || scan.customerTabu(cj)
                            || scan.arcTabu(prev, cj) || scan.arcTabu(ci, next))) {
                        scan.record(new TwoOptMove(r, i, j, delta));
                    }
                }
            }
        }
    }

    /** Bookkeeping of one neighbourhood scan. */
    private static final class Scan {
        final TabuList tabu;
        final int iteration;
        final double bestCost;
        final double currentCost;
        Move best;
        double bestDelta = Double.POSITIVE_INFINITY;

        Scan(State state, TabuList tabu, int iteration, double bestCost) {
            this.tabu = tabu;
            this.iteration = iteration;
            this.bestCost = bestCost;
            this.currentCost = state.cost;
        }

        boolean arcTabu(int i, int j) {
            return tabu.arcUntil[i][j] > iteration;
        }

        boolean routeTabu(int customer, int route) {
            return tabu.routeUntil[customer][route] > iteration;
        }

        boolean customerTabu(int customer) {
            return tabu.customerUntil[customer] > iteration;
        }

        boolean isCandidate(double delta) {
            return delta < bestDelta;
        }

        /** A move is admissible if it is not tabu, or if it aspires (leads to a new best solution). */
        boolean admissible(double delta, boolean tabu) {
            return !tabu || currentCost + delta < bestCost - Solution.EPSILON;
        }

        void record(Move move) {
            best = move;
            bestDelta = move.delta();
        }
    }

    /** Expiry iteration of each tabu attribute; an attribute is tabu while {@code until > iteration}. */
    private final class TabuList {
        /** Symmetric: arc {i, j} may not be rebuilt. */
        final int[][] arcUntil;
        /** Customer c may not re-enter route r. */
        final int[][] routeUntil;
        /** Customer c may not be moved again. */
        final int[] customerUntil;

        TabuList(int nodes, int maxRoutes) {
            arcUntil = new int[nodes][nodes];
            routeUntil = new int[nodes][maxRoutes];
            customerUntil = new int[nodes];
        }

        void forbidCustomer(int iteration, int customer) {
            customerUntil[customer] = tenure(iteration);
        }

        private int tenure(int iteration) {
            return iteration + tabuHorizon + random.nextInt(TENURE_JITTER);
        }

        void forbidArc(int iteration, int i, int j) {
            int until = tenure(iteration);
            arcUntil[i][j] = until;
            arcUntil[j][i] = until;
        }

        void forbidRoute(int iteration, int customer, int route) {
            routeUntil[customer][route] = tenure(iteration);
        }
    }

    private sealed interface Move permits Relocate, Swap, TwoOptMove {
        double delta();
    }

    /** Move the customer at {@code sourceIndex} of route {@code from} after position {@code targetIndex} of route {@code to}. */
    private record Relocate(int from, int sourceIndex, int to, int targetIndex, double delta) implements Move {
    }

    /** Exchange the customer at {@code index1} of route {@code route1} with the one at {@code index2} of route {@code route2}. */
    private record Swap(int route1, int index1, int route2, int index2, double delta) implements Move {
    }

    /** Reverse the segment {@code [i, j]} of route {@code route}. */
    private record TwoOptMove(int route, int i, int j, double delta) implements Move {
    }

    /** Mutable working solution: routes with the depot at both ends, per-route loads and running cost. */
    private final class State {
        final List<List<Integer>> routes = new ArrayList<>();
        int[] load;
        double cost;

        State(Solution initial) {
            reset(initial.getRoutes(), initial.getCost());
        }

        /** Replaces the working solution with the given customer-only routes. */
        void reset(List<List<Integer>> customerRoutes, double newCost) {
            routes.clear();
            load = new int[customerRoutes.size()];
            for (int r = 0; r < customerRoutes.size(); r++) {
                List<Integer> route = new ArrayList<>(customerRoutes.get(r).size() + 2);
                route.add(VrpInstance.DEPOT);
                route.addAll(customerRoutes.get(r));
                route.add(VrpInstance.DEPOT);
                routes.add(route);
                for (int customer : customerRoutes.get(r)) {
                    load[r] += instance.getDemand(customer);
                }
            }
            cost = newCost;
            ensureSpareRoute();
        }

        int firstEmptyRoute() {
            for (int r = 0; r < routes.size(); r++) {
                if (routes.get(r).size() == 2) {
                    return r;
                }
            }
            return -1;
        }

        private void ensureSpareRoute() {
            if (firstEmptyRoute() == -1) {
                routes.add(new ArrayList<>(List.of(VrpInstance.DEPOT, VrpInstance.DEPOT)));
                int[] grown = new int[routes.size()];
                System.arraycopy(load, 0, grown, 0, load.length);
                load = grown;
            }
        }

        void apply(Move move, TabuList tabu, int iteration) {
            switch (move) {
                case Relocate m -> applyRelocate(m, tabu, iteration);
                case Swap m -> applySwap(m, tabu, iteration);
                case TwoOptMove m -> applyTwoOpt(m, tabu, iteration);
            }
            cost += move.delta();
            ensureSpareRoute();
        }

        private void applyRelocate(Relocate move, TabuList tabu, int iteration) {
            List<Integer> source = routes.get(move.from());
            List<Integer> target = routes.get(move.to());
            int customer = source.get(move.sourceIndex());
            int prev = source.get(move.sourceIndex() - 1);
            int next = source.get(move.sourceIndex() + 1);
            int a = target.get(move.targetIndex());
            int b = target.get(move.targetIndex() + 1);

            tabu.forbidArc(iteration, prev, customer);
            tabu.forbidArc(iteration, customer, next);
            tabu.forbidArc(iteration, a, b);
            if (move.from() != move.to()) {
                tabu.forbidRoute(iteration, customer, move.from());
            }
            tabu.forbidCustomer(iteration, customer);

            source.remove(move.sourceIndex());
            int insertAt = move.targetIndex() + 1;
            if (move.from() == move.to() && move.sourceIndex() < move.targetIndex()) {
                insertAt--; // indices after the removed element shifted left
            }
            target.add(insertAt, customer);

            int demand = instance.getDemand(customer);
            load[move.from()] -= demand;
            load[move.to()] += demand;
        }

        private void applySwap(Swap move, TabuList tabu, int iteration) {
            List<Integer> route1 = routes.get(move.route1());
            List<Integer> route2 = routes.get(move.route2());
            int c1 = route1.get(move.index1());
            int c2 = route2.get(move.index2());

            tabu.forbidArc(iteration, route1.get(move.index1() - 1), c1);
            tabu.forbidArc(iteration, c1, route1.get(move.index1() + 1));
            tabu.forbidArc(iteration, route2.get(move.index2() - 1), c2);
            tabu.forbidArc(iteration, c2, route2.get(move.index2() + 1));
            tabu.forbidRoute(iteration, c1, move.route1());
            tabu.forbidRoute(iteration, c2, move.route2());
            tabu.forbidCustomer(iteration, c1);
            tabu.forbidCustomer(iteration, c2);

            route1.set(move.index1(), c2);
            route2.set(move.index2(), c1);

            int diff = instance.getDemand(c2) - instance.getDemand(c1);
            load[move.route1()] += diff;
            load[move.route2()] -= diff;
        }

        private void applyTwoOpt(TwoOptMove move, TabuList tabu, int iteration) {
            List<Integer> route = routes.get(move.route());
            tabu.forbidArc(iteration, route.get(move.i() - 1), route.get(move.i()));
            tabu.forbidArc(iteration, route.get(move.j()), route.get(move.j() + 1));
            tabu.forbidCustomer(iteration, route.get(move.i()));
            tabu.forbidCustomer(iteration, route.get(move.j()));
            Collections.reverse(route.subList(move.i(), move.j() + 1));
        }

        /**
         * Diversification: applies {@code strength} random feasible moves (relocations or swaps)
         * regardless of their cost. The moves go through {@link #apply} so their attributes become
         * tabu and the search cannot simply undo the kick.
         */
        void perturb(TabuList tabu, int iteration, int strength) {
            int applied = 0;
            int attempts = 0;
            while (applied < strength && attempts < 100 * strength) {
                attempts++;
                Move move = random.nextBoolean() ? randomRelocate() : randomSwap();
                if (move != null) {
                    apply(move, tabu, iteration);
                    applied++;
                }
            }
        }

        private int[] randomCustomerPosition() {
            int customer = 1 + random.nextInt(instance.getDimension() - 1);
            for (int r = 0; r < routes.size(); r++) {
                int index = routes.get(r).indexOf(customer);
                if (index >= 0) {
                    return new int[] {r, index};
                }
            }
            throw new IllegalStateException("Customer " + customer + " is not routed");
        }

        private Relocate randomRelocate() {
            int[] pos = randomCustomerPosition();
            int from = pos[0];
            int index = pos[1];
            List<Integer> source = routes.get(from);
            int customer = source.get(index);
            int to = random.nextInt(routes.size());
            if (to == from || load[to] + instance.getDemand(customer) > instance.getCapacity()) {
                return null;
            }
            List<Integer> target = routes.get(to);
            int position = random.nextInt(target.size() - 1);
            int prev = source.get(index - 1);
            int next = source.get(index + 1);
            int a = target.get(position);
            int b = target.get(position + 1);
            double delta = instance.getDistance(a, customer) + instance.getDistance(customer, b)
                    - instance.getDistance(a, b)
                    - instance.getDistance(prev, customer) - instance.getDistance(customer, next)
                    + instance.getDistance(prev, next);
            return new Relocate(from, index, to, position, delta);
        }

        private Swap randomSwap() {
            int[] pos1 = randomCustomerPosition();
            int[] pos2 = randomCustomerPosition();
            int r1 = pos1[0];
            int r2 = pos2[0];
            if (r1 == r2) {
                return null;
            }
            List<Integer> route1 = routes.get(r1);
            List<Integer> route2 = routes.get(r2);
            int i = pos1[1];
            int k = pos2[1];
            int c1 = route1.get(i);
            int c2 = route2.get(k);
            int d1 = instance.getDemand(c1);
            int d2 = instance.getDemand(c2);
            if (load[r1] - d1 + d2 > instance.getCapacity() || load[r2] - d2 + d1 > instance.getCapacity()) {
                return null;
            }
            int p1 = route1.get(i - 1);
            int n1 = route1.get(i + 1);
            int p2 = route2.get(k - 1);
            int n2 = route2.get(k + 1);
            double delta = instance.getDistance(p1, c2) + instance.getDistance(c2, n1)
                    - instance.getDistance(p1, c1) - instance.getDistance(c1, n1)
                    + instance.getDistance(p2, c1) + instance.getDistance(c1, n2)
                    - instance.getDistance(p2, c2) - instance.getDistance(c2, n2);
            return new Swap(r1, i, r2, k, delta);
        }

        List<List<Integer>> copyRoutes() {
            List<List<Integer>> copy = new ArrayList<>();
            for (List<Integer> route : routes) {
                if (route.size() > 2) {
                    copy.add(new ArrayList<>(route.subList(1, route.size() - 1)));
                }
            }
            return copy;
        }
    }
}
