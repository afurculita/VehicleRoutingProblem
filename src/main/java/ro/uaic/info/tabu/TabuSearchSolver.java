package ro.uaic.info.tabu;

import ro.uaic.info.Solution;
import ro.uaic.info.Solver;
import ro.uaic.info.VrpInstance;
import ro.uaic.info.greedy.ClarkeWrightSolver;
import ro.uaic.info.local.CandidateLists;
import ro.uaic.info.local.Move;
import ro.uaic.info.local.Neighbourhood;
import ro.uaic.info.local.RouteState;

import java.util.List;
import java.util.Random;

/**
 * Tabu Search for the CVRP.
 *
 * <p>The search starts from the {@link ClarkeWrightSolver savings} solution and, at every iteration,
 * scans the (granular) {@link Neighbourhood}: relocate / Or-opt, swap, 2-opt and cross-exchange.
 * Capacity and route-length constraints are always respected. The best admissible move is applied
 * even when it worsens the solution, which is what lets the search leave local optima.
 *
 * <p>Short-term memory (tabu list), in the spirit of Osman's and Gendreau, Hertz &amp; Laporte's tabu
 * searches for the VRP:
 * <ul>
 *   <li>a customer that has just been moved may not be moved again;</li>
 *   <li>a customer that has just left a route may not re-enter that route, which stops a customer
 *       from oscillating between two routes;</li>
 *   <li>an arc that has just been broken may not be rebuilt.</li>
 * </ul>
 * The tenure is {@code tabuHorizon} plus a random offset in {@code [0, 5)}. Because every iteration
 * freezes two or three customers, short tenures (5–10) work best. The standard aspiration criterion
 * applies: a tabu move is allowed if it yields a new best solution.
 *
 * <p>Long-term memory: a worsening move is penalised in proportion to how often the customers it
 * moves have already been moved (frequency-based diversification, Taillard 1993). With
 * {@code penalty = λ}, a customer moved {@code f} times in {@code t} iterations adds
 * {@code λ · (cost / n) · (f / t) · (n / 2)} to the score of a non-improving move, i.e. a customer
 * moved at the average rate adds {@code λ} times the average cost per customer.
 *
 * <p>Diversification: after every {@value #STAGNATION_BEFORE_PERTURBATION} consecutive non-improving
 * iterations the search restarts from the best solution perturbed by a few random moves, whose
 * attributes are made tabu so the kick cannot be undone right away.
 *
 * <p>The search stops after {@code maxIterationsWithoutImprovement} consecutive iterations that did
 * not improve the best known solution.
 *
 * <p>Runs are reproducible for a given {@link Random} seed.
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
    private final double penalty;
    private final Random random;
    private final Neighbourhood neighbourhood;

    /**
     * Complete neighbourhood, no frequency penalty.
     *
     * @param instance                        the problem
     * @param tabuHorizon                     base tabu tenure, in iterations (>= 0)
     * @param maxIterationsWithoutImprovement stopping criterion (>= 1)
     * @param random                          source of randomness for the tenure jitter and the perturbation
     */
    public TabuSearchSolver(VrpInstance instance, int tabuHorizon, int maxIterationsWithoutImprovement, Random random) {
        this(instance, tabuHorizon, maxIterationsWithoutImprovement, 0.0, random, new CandidateLists(instance, 0));
    }

    /**
     * @param penalty    frequency penalty factor λ (>= 0, 0 disables the long-term memory)
     * @param candidates candidate lists defining the granular neighbourhood
     */
    public TabuSearchSolver(VrpInstance instance, int tabuHorizon, int maxIterationsWithoutImprovement, double penalty,
                            Random random, CandidateLists candidates) {
        if (tabuHorizon < 0) {
            throw new IllegalArgumentException("tabuHorizon must be >= 0, got " + tabuHorizon);
        }
        if (maxIterationsWithoutImprovement < 1) {
            throw new IllegalArgumentException("maxIterationsWithoutImprovement must be >= 1, got " + maxIterationsWithoutImprovement);
        }
        if (Double.isNaN(penalty) || penalty < 0) {
            throw new IllegalArgumentException("penalty must be >= 0, got " + penalty);
        }
        this.instance = instance;
        this.tabuHorizon = tabuHorizon;
        this.maxIterationsWithoutImprovement = maxIterationsWithoutImprovement;
        this.penalty = penalty;
        this.random = random;
        this.neighbourhood = new Neighbourhood(instance, candidates);
    }

    @Override
    public Solution solve() {
        return solveFrom(new ClarkeWrightSolver(instance).solve());
    }

    /** Runs the search starting from the given feasible solution. */
    public Solution solveFrom(Solution initial) {
        RouteState state = new RouteState(instance, initial);
        Memory memory = new Memory(instance.getDimension(), instance.getDimension() + 1);

        List<List<Integer>> bestRoutes = state.toCustomerRoutes();
        double bestCost = state.cost();
        int iteration = 0;
        int stagnation = 0;

        while (stagnation < maxIterationsWithoutImprovement) {
            final int it = iteration;
            final double currentCost = state.cost();
            final double best = bestCost;
            Move move = neighbourhood.best(state, m -> score(m, memory, it, currentCost, best));
            iteration++;
            if (move == null) {
                // Every move is tabu and none aspires (tiny, tightly constrained neighbourhoods): kick instead.
                state.reset(bestRoutes);
                if (perturb(state, memory, iteration, Math.max(3, instance.getNumberOfCustomers() / PERTURBATION_DIVISOR)) == 0) {
                    break;
                }
                stagnation++;
                continue;
            }
            memory.record(move, state, iteration);
            state.apply(move);

            if (state.cost() < bestCost - Solution.EPSILON) {
                bestCost = state.cost();
                bestRoutes = state.toCustomerRoutes();
                stagnation = 0;
            } else {
                stagnation++;
                if (stagnation % STAGNATION_BEFORE_PERTURBATION == 0 && stagnation < maxIterationsWithoutImprovement) {
                    state.reset(bestRoutes);
                    perturb(state, memory, iteration, Math.max(3, instance.getNumberOfCustomers() / PERTURBATION_DIVISOR));
                }
            }
        }
        return Solution.of(instance, bestRoutes);
    }

    /** Tabu status, aspiration and frequency penalty of a feasible move; NaN rejects it. */
    private double score(Move move, Memory memory, int iteration, double currentCost, double bestCost) {
        double delta = move.delta();
        boolean aspires = currentCost + delta < bestCost - Solution.EPSILON;
        if (!aspires && memory.isTabu(move, iteration)) {
            return Double.NaN;
        }
        if (penalty == 0.0 || delta < -Solution.EPSILON || iteration == 0) {
            return delta;
        }
        double averageCostPerCustomer = currentCost / instance.getNumberOfCustomers();
        double sum = 0.0;
        for (int customer : move.movedCustomers()) {
            sum += memory.frequency[customer];
        }
        return delta + penalty * averageCostPerCustomer * sum / iteration * instance.getNumberOfCustomers() / 2.0;
    }

    /**
     * Diversification: applies {@code strength} random feasible relocations or swaps regardless of
     * their cost, recording them in the memory so the search cannot simply undo the kick.
     *
     * @return the number of moves actually applied
     */
    private int perturb(RouteState state, Memory memory, int iteration, int strength) {
        int applied = 0;
        int attempts = 0;
        while (applied < strength && attempts < 100 * strength) {
            attempts++;
            Move move = random.nextBoolean() ? randomRelocate(state) : randomSwap(state);
            if (move != null) {
                memory.record(move, state, iteration);
                state.apply(move);
                applied++;
            }
        }
        return applied;
    }

    private Move randomRelocate(RouteState state) {
        int customer = 1 + random.nextInt(instance.getDimension() - 1);
        int to = random.nextInt(state.numberOfRoutes());
        int after = random.nextInt(state.route(to).size() - 1);
        return neighbourhood.relocate(state, state.routeOf(customer), state.positionOf(customer), 1, false, to, after);
    }

    private Move randomSwap(RouteState state) {
        int c1 = 1 + random.nextInt(instance.getDimension() - 1);
        int c2 = 1 + random.nextInt(instance.getDimension() - 1);
        if (c1 == c2) {
            return null;
        }
        return neighbourhood.swap(state, state.routeOf(c1), state.positionOf(c1), state.routeOf(c2), state.positionOf(c2));
    }

    /** Short-term (tabu attributes with expiry iteration) and long-term (move frequencies) memory. */
    private final class Memory {
        /** Symmetric: arc {i, j} may not be rebuilt while {@code arcUntil > iteration}. */
        final int[][] arcUntil;
        /** Customer c may not re-enter route r. */
        final int[][] routeUntil;
        /** Customer c may not be moved again. */
        final int[] customerUntil;
        /** How many times each customer has been moved. */
        final int[] frequency;

        Memory(int nodes, int maxRoutes) {
            arcUntil = new int[nodes][nodes];
            routeUntil = new int[nodes][maxRoutes];
            customerUntil = new int[nodes];
            frequency = new int[nodes];
        }

        boolean isTabu(Move move, int iteration) {
            for (int customer : move.movedCustomers()) {
                if (customerUntil[customer] > iteration) {
                    return true;
                }
            }
            for (int[] arc : move.arcsCreated()) {
                if (arcUntil[arc[0]][arc[1]] > iteration) {
                    return true;
                }
            }
            return switch (move) {
                case Move.Relocate m -> !m.sameRoute() && anyRouteTabu(m.segment(), m.to(), iteration);
                case Move.Swap m -> !m.sameRoute()
                        && (routeUntil[m.customer1()][m.route2()] > iteration || routeUntil[m.customer2()][m.route1()] > iteration);
                case Move.TwoOpt m -> false;
                case Move.CrossExchange m -> (m.tail1() != VrpInstance.DEPOT && routeUntil[m.tail1()][m.route2()] > iteration)
                        || (m.tail2() != VrpInstance.DEPOT && routeUntil[m.tail2()][m.route1()] > iteration);
            };
        }

        private boolean anyRouteTabu(int[] customers, int route, int iteration) {
            for (int customer : customers) {
                if (routeUntil[customer][route] > iteration) {
                    return true;
                }
            }
            return false;
        }

        /** Makes the attributes reversed by {@code move} (about to be applied to {@code state}) tabu. */
        void record(Move move, RouteState state, int iteration) {
            for (int customer : move.movedCustomers()) {
                customerUntil[customer] = tenure(iteration);
                frequency[customer]++;
            }
            for (int[] arc : move.arcsRemoved()) {
                int until = tenure(iteration);
                arcUntil[arc[0]][arc[1]] = until;
                arcUntil[arc[1]][arc[0]] = until;
            }
            switch (move) {
                case Move.Relocate m -> {
                    if (!m.sameRoute()) {
                        for (int customer : m.segment()) {
                            routeUntil[customer][m.from()] = tenure(iteration);
                        }
                    }
                }
                case Move.Swap m -> {
                    if (!m.sameRoute()) {
                        routeUntil[m.customer1()][m.route1()] = tenure(iteration);
                        routeUntil[m.customer2()][m.route2()] = tenure(iteration);
                    }
                }
                case Move.TwoOpt m -> {
                    // intra-route: arcs and customers are enough
                }
                case Move.CrossExchange m -> {
                    if (m.tail1() != VrpInstance.DEPOT) {
                        routeUntil[m.tail1()][m.route1()] = tenure(iteration);
                    }
                    if (m.tail2() != VrpInstance.DEPOT) {
                        routeUntil[m.tail2()][m.route2()] = tenure(iteration);
                    }
                }
            }
        }

        private int tenure(int iteration) {
            return iteration + tabuHorizon + random.nextInt(TENURE_JITTER);
        }
    }
}
