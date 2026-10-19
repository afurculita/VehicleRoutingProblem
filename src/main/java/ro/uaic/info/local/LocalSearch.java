package ro.uaic.info.local;

import ro.uaic.info.VrpInstance;

import java.util.List;

/**
 * Best-improvement descent over the {@link Neighbourhood}: repeatedly applies the most improving
 * move until none is left, which yields a solution that is locally optimal with respect to
 * relocate, Or-opt, swap, 2-opt and cross-exchange.
 */
public final class LocalSearch {

    private final Neighbourhood neighbourhood;

    public LocalSearch(Neighbourhood neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    public LocalSearch(VrpInstance instance, CandidateLists candidates) {
        this(new Neighbourhood(instance, candidates));
    }

    public Neighbourhood neighbourhood() {
        return neighbourhood;
    }

    /**
     * Improves {@code state} in place until it is locally optimal.
     *
     * @return the total cost reduction (>= 0)
     */
    public double descend(RouteState state) {
        double before = state.cost();
        Move move;
        while ((move = neighbourhood.best(state, MoveEvaluator.IMPROVING)) != null) {
            state.apply(move);
        }
        return before - state.cost();
    }

    /** Convenience: returns locally optimal routes derived from {@code customerRoutes} (which are not modified). */
    public List<List<Integer>> improve(VrpInstance instance, List<? extends List<Integer>> customerRoutes) {
        RouteState state = new RouteState(instance, customerRoutes);
        descend(state);
        return state.toCustomerRoutes();
    }
}
