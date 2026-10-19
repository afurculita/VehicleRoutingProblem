package ro.uaic.info;

/**
 * A CVRP solver. Implementations are constructed with a {@link VrpInstance} (and their parameters)
 * and produce a feasible {@link Solution} on each call to {@link #solve()}.
 */
public interface Solver {

    /**
     * Runs the algorithm.
     *
     * @return a feasible solution of the instance the solver was built for
     */
    Solution solve();
}
