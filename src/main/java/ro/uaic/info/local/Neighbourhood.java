package ro.uaic.info.local;

import ro.uaic.info.VrpInstance;

import java.util.List;

/**
 * The move neighbourhood shared by the local search and the tabu search: segment relocation
 * (relocate / Or-opt, segments of 1 to {@value #MAX_SEGMENT} customers, both orientations),
 * swap (1-1 exchange, intra- and inter-route), 2-opt (intra-route reversal) and cross-exchange
 * (2-opt*, tail exchange between two routes).
 *
 * <p>All moves respect the vehicle capacity and, when the instance declares one, the maximum route
 * length. The neighbourhood is <em>granular</em>: only moves creating at least one arc between a
 * node and one of its {@link CandidateLists candidates} are generated, plus every move involving the
 * depot. With complete candidate lists the whole neighbourhood is scanned.
 */
public final class Neighbourhood {

    public static final double EPSILON = 1e-9;
    /** Longest segment moved by Or-opt. */
    public static final int MAX_SEGMENT = 3;

    private final VrpInstance instance;
    private final CandidateLists candidates;
    private final double maxLength;

    public Neighbourhood(VrpInstance instance, CandidateLists candidates) {
        this.instance = instance;
        this.candidates = candidates;
        this.maxLength = instance.getMaxRouteLength() + EPSILON;
    }

    public CandidateLists candidates() {
        return candidates;
    }

    /** Scans the neighbourhood of {@code state} and returns the acceptable move with the lowest score, or null. */
    public Move best(RouteState state, MoveEvaluator evaluator) {
        Scan scan = new Scan(state, evaluator);
        scanRelocations(scan);
        scanSwaps(scan);
        scanTwoOpt(scan);
        scanCrossExchange(scan);
        return scan.best;
    }

    // ---------------------------------------------------------------- move factories

    /**
     * Builds the relocation of segment {@code [start, start + length)} of route {@code from} after
     * position {@code after} of route {@code to}, or null if it is a no-op or infeasible.
     */
    public Move.Relocate relocate(RouteState s, int from, int start, int length, boolean reversed, int to, int after) {
        List<Integer> source = s.route(from);
        List<Integer> target = s.route(to);
        int end = start + length - 1;
        if (start < 1 || end > source.size() - 2 || after < 0 || after > target.size() - 2) {
            return null;
        }
        if (from == to && after >= start - 1 && after <= end) {
            return null;
        }
        int prev = source.get(start - 1);
        int next = source.get(end + 1);
        int before = target.get(after);
        int following = target.get(after + 1);
        int demand = 0;
        double inner = 0.0; // length of the arcs inside the segment: moves with it to the target route
        for (int p = start; p <= end; p++) {
            demand += instance.getDemand(source.get(p));
            if (p > start) {
                inner += instance.getDistance(source.get(p - 1), source.get(p));
            }
        }
        if (from != to && s.load(to) + demand > instance.getCapacity()) {
            return null;
        }
        int[] segment = new int[length];
        for (int p = 0; p < length; p++) {
            segment[p] = source.get(reversed ? end - p : start + p);
        }
        double removalGain = instance.getDistance(prev, source.get(start)) + instance.getDistance(source.get(end), next)
                - instance.getDistance(prev, next);
        // the arcs inside the segment cancel out in the total cost, only the three boundary arcs matter
        double insertion = instance.getDistance(before, segment[0])
                + instance.getDistance(segment[length - 1], following) - instance.getDistance(before, following);
        if (!lengthOk(s, from, to, insertion + inner, insertion - removalGain)) {
            return null;
        }
        return new Move.Relocate(from, start, length, reversed, to, after, segment, prev, next, before, following,
                insertion - removalGain);
    }

    /** Builds the exchange of the customers at {@code (r1, i)} and {@code (r2, k)}, or null if no-op or infeasible. */
    public Move.Swap swap(RouteState s, int r1, int i, int r2, int k) {
        List<Integer> route1 = s.route(r1);
        List<Integer> route2 = s.route(r2);
        if (i < 1 || i > route1.size() - 2 || k < 1 || k > route2.size() - 2) {
            return null;
        }
        if (r1 == r2 && Math.abs(i - k) < 2) {
            return null;
        }
        int c1 = route1.get(i);
        int c2 = route2.get(k);
        int d1 = instance.getDemand(c1);
        int d2 = instance.getDemand(c2);
        if (r1 != r2 && (s.load(r1) - d1 + d2 > instance.getCapacity() || s.load(r2) - d2 + d1 > instance.getCapacity())) {
            return null;
        }
        int p1 = route1.get(i - 1);
        int n1 = route1.get(i + 1);
        int p2 = route2.get(k - 1);
        int n2 = route2.get(k + 1);
        double delta1 = instance.getDistance(p1, c2) + instance.getDistance(c2, n1)
                - instance.getDistance(p1, c1) - instance.getDistance(c1, n1);
        double delta2 = instance.getDistance(p2, c1) + instance.getDistance(c1, n2)
                - instance.getDistance(p2, c2) - instance.getDistance(c2, n2);
        if (r1 != r2 ? s.length(r1) + delta1 > maxLength || s.length(r2) + delta2 > maxLength
                : s.length(r1) + delta1 + delta2 > maxLength) {
            return null;
        }
        return new Move.Swap(r1, i, r2, k, c1, c2, p1, n1, p2, n2, delta1 + delta2);
    }

    /** Builds the reversal of {@code [i, j]} in route {@code r}, or null if no-op or infeasible. */
    public Move.TwoOpt twoOpt(RouteState s, int r, int i, int j) {
        List<Integer> route = s.route(r);
        if (i < 1 || j >= route.size() - 1 || j <= i || (i == 1 && j == route.size() - 2)) {
            return null;
        }
        int prev = route.get(i - 1);
        int first = route.get(i);
        int last = route.get(j);
        int next = route.get(j + 1);
        double delta = instance.getDistance(prev, last) + instance.getDistance(first, next)
                - instance.getDistance(prev, first) - instance.getDistance(last, next);
        if (s.length(r) + delta > maxLength) {
            return null;
        }
        return new Move.TwoOpt(r, i, j, prev, first, last, next, delta);
    }

    // ---------------------------------------------------------------- scanning

    /** Per-scan bookkeeping: best move so far and prefix sums for the cross-exchange. */
    private final class Scan {
        final RouteState s;
        final MoveEvaluator evaluator;
        Move best;
        double bestScore = Double.POSITIVE_INFINITY;
        /** prefixLoad[r][p] = load of route r up to position p; prefixLength[r][p] = length up to p. */
        final int[][] prefixLoad;
        final double[][] prefixLength;

        Scan(RouteState s, MoveEvaluator evaluator) {
            this.s = s;
            this.evaluator = evaluator;
            prefixLoad = new int[s.numberOfRoutes()][];
            prefixLength = new double[s.numberOfRoutes()][];
            for (int r = 0; r < s.numberOfRoutes(); r++) {
                List<Integer> route = s.route(r);
                prefixLoad[r] = new int[route.size()];
                prefixLength[r] = new double[route.size()];
                for (int p = 1; p < route.size(); p++) {
                    prefixLoad[r][p] = prefixLoad[r][p - 1] + instance.getDemand(route.get(p));
                    prefixLength[r][p] = prefixLength[r][p - 1] + instance.getDistance(route.get(p - 1), route.get(p));
                }
            }
        }

        boolean promising(double delta) {
            return delta < bestScore;
        }

        void offer(Move move) {
            if (move == null) {
                return;
            }
            double score = evaluator.score(move);
            if (!Double.isNaN(score) && score < bestScore) {
                bestScore = score;
                best = move;
            }
        }
    }

    private void scanRelocations(Scan scan) {
        RouteState s = scan.s;
        int routes = s.numberOfRoutes();
        for (int from = 0; from < routes; from++) {
            List<Integer> source = s.route(from);
            for (int start = 1; start <= source.size() - 2; start++) {
                int prev = source.get(start - 1);
                int first = source.get(start);
                int demand = 0;
                double inner = 0.0;
                for (int length = 1; length <= MAX_SEGMENT && start + length - 1 <= source.size() - 2; length++) {
                    int end = start + length - 1;
                    int last = source.get(end);
                    demand += instance.getDemand(last);
                    if (length > 1) {
                        inner += instance.getDistance(source.get(end - 1), last);
                    }
                    int next = source.get(end + 1);
                    double removalGain = instance.getDistance(prev, first) + instance.getDistance(last, next)
                            - instance.getDistance(prev, next);

                    // positions next to a candidate of either end of the segment
                    for (int endpoint : new int[] {first, last}) {
                        for (int a : candidates.of(endpoint)) {
                            if (a == VrpInstance.DEPOT) {
                                continue;
                            }
                            int to = s.routeOf(a);
                            int pos = s.positionOf(a);
                            tryRelocate(scan, from, start, length, demand, inner, removalGain, to, pos);
                            tryRelocate(scan, from, start, length, demand, inner, removalGain, to, pos - 1);
                        }
                        if (length == 1) {
                            break; // first == last
                        }
                    }
                    // positions next to the depot, in every route
                    for (int to = 0; to < routes; to++) {
                        int size = s.route(to).size();
                        tryRelocate(scan, from, start, length, demand, inner, removalGain, to, 0);
                        if (size > 2) {
                            tryRelocate(scan, from, start, length, demand, inner, removalGain, to, size - 2);
                        }
                    }
                }
            }
        }
    }

    private void tryRelocate(Scan scan, int from, int start, int length, int demand, double inner,
                             double removalGain, int to, int after) {
        RouteState s = scan.s;
        List<Integer> source = s.route(from);
        List<Integer> target = s.route(to);
        int end = start + length - 1;
        if (after < 0 || after > target.size() - 2 || (from == to && after >= start - 1 && after <= end)) {
            return;
        }
        if (from != to && s.load(to) + demand > instance.getCapacity()) {
            return;
        }
        int before = target.get(after);
        int following = target.get(after + 1);
        int first = source.get(start);
        int last = source.get(end);
        double base = -instance.getDistance(before, following) - removalGain;
        double forward = base + instance.getDistance(before, first) + instance.getDistance(last, following);
        if (scan.promising(forward) && lengthOk(s, from, to, forward + removalGain + inner, forward)) {
            scan.offer(relocate(s, from, start, length, false, to, after));
        }
        if (length > 1) {
            double reversed = base + instance.getDistance(before, last) + instance.getDistance(first, following);
            if (scan.promising(reversed) && lengthOk(s, from, to, reversed + removalGain + inner, reversed)) {
                scan.offer(relocate(s, from, start, length, true, to, after));
            }
        }
    }

    /**
     * Route-length check of a segment relocation.
     *
     * @param targetGrowth length added to the target route when the routes differ (boundary arcs plus inner arcs)
     * @param delta        total cost change, which is also the source route's change when the routes are the same
     */
    private boolean lengthOk(RouteState s, int from, int to, double targetGrowth, double delta) {
        if (maxLength == Double.POSITIVE_INFINITY) {
            return true;
        }
        return from != to ? s.length(to) + targetGrowth <= maxLength : s.length(from) + delta <= maxLength;
    }

    private void scanSwaps(Scan scan) {
        RouteState s = scan.s;
        int n = instance.getDimension();
        for (int c1 = 1; c1 < n; c1++) {
            int r1 = s.routeOf(c1);
            int i = s.positionOf(c1);
            for (int a : candidates.of(c1)) {
                if (a == VrpInstance.DEPOT) {
                    // c2 adjacent to the depot: first or last customer of any route
                    for (int r2 = 0; r2 < s.numberOfRoutes(); r2++) {
                        int size = s.route(r2).size();
                        if (size > 2) {
                            trySwap(scan, r1, i, r2, 1);
                            trySwap(scan, r1, i, r2, size - 2);
                        }
                    }
                } else {
                    int r2 = s.routeOf(a);
                    int pos = s.positionOf(a);
                    trySwap(scan, r1, i, r2, pos - 1);
                    trySwap(scan, r1, i, r2, pos + 1);
                }
            }
        }
    }

    private void trySwap(Scan scan, int r1, int i, int r2, int k) {
        RouteState s = scan.s;
        List<Integer> route1 = s.route(r1);
        List<Integer> route2 = s.route(r2);
        if (k < 1 || k > route2.size() - 2 || (r1 == r2 && Math.abs(i - k) < 2)) {
            return;
        }
        int c1 = route1.get(i);
        int c2 = route2.get(k);
        double delta = instance.getDistance(route1.get(i - 1), c2) + instance.getDistance(c2, route1.get(i + 1))
                - instance.getDistance(route1.get(i - 1), c1) - instance.getDistance(c1, route1.get(i + 1))
                + instance.getDistance(route2.get(k - 1), c1) + instance.getDistance(c1, route2.get(k + 1))
                - instance.getDistance(route2.get(k - 1), c2) - instance.getDistance(c2, route2.get(k + 1));
        if (scan.promising(delta)) {
            scan.offer(swap(s, r1, i, r2, k)); // re-checks capacity and length
        }
    }

    private void scanTwoOpt(Scan scan) {
        RouteState s = scan.s;
        for (int r = 0; r < s.numberOfRoutes(); r++) {
            List<Integer> route = s.route(r);
            int size = route.size();
            if (size < 4) {
                continue;
            }
            // arc (route[p], route[q]) created with i = p + 1, j = q
            for (int q = 2; q <= size - 2; q++) {
                tryTwoOpt(scan, r, 1, q);
            }
            for (int p = 1; p <= size - 3; p++) {
                int u = route.get(p);
                for (int v : candidates.of(u)) {
                    if (v == VrpInstance.DEPOT) {
                        tryTwoOpt(scan, r, p, size - 2); // arc (u, depot) created with j = size - 2
                    } else if (s.routeOf(v) == r) {
                        int q = s.positionOf(v);
                        if (q >= p + 2) {
                            tryTwoOpt(scan, r, p + 1, q); // arc (u, v) as (prev, last)
                            tryTwoOpt(scan, r, p, q - 1);  // arc (u, v) as (first, next)
                        }
                    }
                }
            }
        }
    }

    private void tryTwoOpt(Scan scan, int r, int i, int j) {
        RouteState s = scan.s;
        List<Integer> route = s.route(r);
        if (j <= i || i < 1 || j > route.size() - 2 || (i == 1 && j == route.size() - 2)) {
            return;
        }
        double delta = instance.getDistance(route.get(i - 1), route.get(j)) + instance.getDistance(route.get(i), route.get(j + 1))
                - instance.getDistance(route.get(i - 1), route.get(i)) - instance.getDistance(route.get(j), route.get(j + 1));
        if (scan.promising(delta)) {
            scan.offer(twoOpt(s, r, i, j));
        }
    }

    private void scanCrossExchange(Scan scan) {
        RouteState s = scan.s;
        int n = instance.getDimension();
        int routes = s.numberOfRoutes();
        for (int u = 1; u < n; u++) {
            int r1 = s.routeOf(u);
            int i = s.positionOf(u);
            for (int v : candidates.of(u)) {
                if (v == VrpInstance.DEPOT) {
                    for (int r2 = 0; r2 < routes; r2++) {
                        if (r2 != r1) {
                            tryCross(scan, r1, i, r2, s.route(r2).size() - 2); // arc (u, depot)
                            tryCross(scan, r1, i - 1, r2, 0);                  // arc (depot, u)
                        }
                    }
                } else {
                    int r2 = s.routeOf(v);
                    if (r2 != r1) {
                        int q = s.positionOf(v);
                        tryCross(scan, r1, i, r2, q - 1); // arc (u, v) as (head1, tail2)
                        tryCross(scan, r1, i - 1, r2, q); // arc (v, u) as (head2, tail1)
                    }
                }
            }
        }
        // move the whole tail of a route after any position of another route (head1 = depot)
        for (int r1 = 0; r1 < routes; r1++) {
            if (s.route(r1).size() == 2) {
                continue;
            }
            for (int r2 = 0; r2 < routes; r2++) {
                if (r2 == r1) {
                    continue;
                }
                for (int k = 1; k <= s.route(r2).size() - 2; k++) {
                    tryCross(scan, r1, 0, r2, k);
                }
            }
        }
    }

    private void tryCross(Scan scan, int r1, int i, int r2, int k) {
        RouteState s = scan.s;
        List<Integer> route1 = s.route(r1);
        List<Integer> route2 = s.route(r2);
        int size1 = route1.size();
        int size2 = route2.size();
        if (i < 0 || k < 0 || i > size1 - 2 || k > size2 - 2) {
            return;
        }
        if ((i == size1 - 2 && k == size2 - 2) || (i == 0 && k == 0)) {
            return; // exchanging empty tails or whole routes changes nothing
        }
        int head1 = route1.get(i);
        int tail1 = route1.get(i + 1);
        int head2 = route2.get(k);
        int tail2 = route2.get(k + 1);
        double delta = instance.getDistance(head1, tail2) + instance.getDistance(head2, tail1)
                - instance.getDistance(head1, tail1) - instance.getDistance(head2, tail2);
        if (!scan.promising(delta)) {
            return;
        }
        int newLoad1 = scan.prefixLoad[r1][i] + s.load(r2) - scan.prefixLoad[r2][k];
        int newLoad2 = scan.prefixLoad[r2][k] + s.load(r1) - scan.prefixLoad[r1][i];
        if (newLoad1 > instance.getCapacity() || newLoad2 > instance.getCapacity()) {
            return;
        }
        if (maxLength != Double.POSITIVE_INFINITY) {
            double newLength1 = scan.prefixLength[r1][i] + instance.getDistance(head1, tail2)
                    + s.length(r2) - scan.prefixLength[r2][k + 1];
            double newLength2 = scan.prefixLength[r2][k] + instance.getDistance(head2, tail1)
                    + s.length(r1) - scan.prefixLength[r1][i + 1];
            if (newLength1 > maxLength || newLength2 > maxLength) {
                return;
            }
        }
        scan.offer(new Move.CrossExchange(r1, i, r2, k, head1, tail1, head2, tail2, delta));
    }
}
