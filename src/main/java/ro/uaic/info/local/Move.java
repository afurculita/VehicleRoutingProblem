package ro.uaic.info.local;

/**
 * A move of the inter/intra-route neighbourhood applied to a {@link RouteState}.
 *
 * <p>Route indices and positions refer to the state the move was generated for. Besides the data
 * needed to apply the move, every record carries the node ids around the modified arcs so that a
 * tabu search can derive its attributes (customers moved, arcs created and destroyed) without
 * re-reading the routes.
 */
public sealed interface Move {

    /** Cost change of the whole solution when the move is applied (negative = improvement). */
    double delta();

    /** Customers whose position changes (never the depot). */
    int[] movedCustomers();

    /** Arcs {i, j} the move creates. */
    int[][] arcsCreated();

    /** Arcs {i, j} the move destroys. */
    int[][] arcsRemoved();

    /**
     * Moves the segment {@code [start, start + length)} of route {@code from} to just after position
     * {@code after} of route {@code to}, optionally reversed (Or-opt; a single customer is the classic
     * relocate / 1-0 exchange).
     *
     * @param segment   the segment nodes in insertion order (already reversed when {@code reversed})
     * @param prev      node before the segment in the source route
     * @param next      node after the segment in the source route
     * @param before    node at position {@code after} of the target route
     * @param following node at position {@code after + 1} of the target route
     */
    record Relocate(int from, int start, int length, boolean reversed, int to, int after, int[] segment,
                    int prev, int next, int before, int following, double delta) implements Move {

        public boolean sameRoute() {
            return from == to;
        }

        @Override
        public int[] movedCustomers() {
            return segment.clone();
        }

        @Override
        public int[][] arcsCreated() {
            return new int[][] {{prev, next}, {before, segment[0]}, {segment[segment.length - 1], following}};
        }

        @Override
        public int[][] arcsRemoved() {
            int first = reversed ? segment[segment.length - 1] : segment[0];
            int last = reversed ? segment[0] : segment[segment.length - 1];
            return new int[][] {{prev, first}, {last, next}, {before, following}};
        }
    }

    /** Exchanges customer {@code customer1} (route {@code route1}) with {@code customer2} (route {@code route2}). */
    record Swap(int route1, int index1, int route2, int index2, int customer1, int customer2,
                int prev1, int next1, int prev2, int next2, double delta) implements Move {

        public boolean sameRoute() {
            return route1 == route2;
        }

        @Override
        public int[] movedCustomers() {
            return new int[] {customer1, customer2};
        }

        @Override
        public int[][] arcsCreated() {
            return new int[][] {{prev1, customer2}, {customer2, next1}, {prev2, customer1}, {customer1, next2}};
        }

        @Override
        public int[][] arcsRemoved() {
            return new int[][] {{prev1, customer1}, {customer1, next1}, {prev2, customer2}, {customer2, next2}};
        }
    }

    /** Reverses the segment {@code [i, j]} of route {@code route} (arcs (prev, first) and (last, next) are replaced). */
    record TwoOpt(int route, int i, int j, int prev, int first, int last, int next, double delta) implements Move {

        @Override
        public int[] movedCustomers() {
            return new int[] {first, last};
        }

        @Override
        public int[][] arcsCreated() {
            return new int[][] {{prev, last}, {first, next}};
        }

        @Override
        public int[][] arcsRemoved() {
            return new int[][] {{prev, first}, {last, next}};
        }
    }

    /**
     * Exchanges the tails of two routes (2-opt*): route 1 becomes {@code route1[0..index1] + route2[index2+1..]}
     * and route 2 becomes {@code route2[0..index2] + route1[index1+1..]}.
     *
     * @param head1 node at {@code index1} of route 1
     * @param tail1 node at {@code index1 + 1} of route 1 (first node of the tail that moves to route 2)
     * @param head2 node at {@code index2} of route 2
     * @param tail2 node at {@code index2 + 1} of route 2 (first node of the tail that moves to route 1)
     */
    record CrossExchange(int route1, int index1, int route2, int index2, int head1, int tail1, int head2, int tail2,
                         double delta) implements Move {

        @Override
        public int[] movedCustomers() {
            if (tail1 == 0) {
                return tail2 == 0 ? new int[0] : new int[] {tail2};
            }
            return tail2 == 0 ? new int[] {tail1} : new int[] {tail1, tail2};
        }

        @Override
        public int[][] arcsCreated() {
            return new int[][] {{head1, tail2}, {head2, tail1}};
        }

        @Override
        public int[][] arcsRemoved() {
            return new int[][] {{head1, tail1}, {head2, tail2}};
        }
    }
}
