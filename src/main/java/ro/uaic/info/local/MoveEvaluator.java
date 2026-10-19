package ro.uaic.info.local;

/**
 * Decides whether a feasible move is acceptable and how it ranks.
 *
 * <p>{@link Neighbourhood#best} returns the acceptable move with the lowest score. A score must be
 * greater than or equal to the move's {@link Move#delta()} (scores only add penalties), which lets
 * the scanner discard candidates by their delta before building the move object.
 */
@FunctionalInterface
public interface MoveEvaluator {

    /** Score to minimise, or {@link Double#NaN} to reject the move. */
    double score(Move move);

    /** Accepts strictly improving moves, ranked by their delta: a plain best-improvement descent. */
    MoveEvaluator IMPROVING = move -> move.delta() < -Neighbourhood.EPSILON ? move.delta() : Double.NaN;
}
