package ro.uaic.info.greedy;

import ro.uaic.info.Solution;
import ro.uaic.info.Solver;
import ro.uaic.info.VrpInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Nearest-neighbour construction heuristic for the CVRP.
 *
 * <p>Starting at the depot, the vehicle repeatedly drives to the closest unserved customer whose
 * demand still fits in the remaining capacity and that can still be served within the maximum route
 * length, if any. When no customer fits, the route is closed and a new vehicle starts from the
 * depot. The number of vehicles is not bounded; a feasible solution always exists because every
 * single customer can be served by one vehicle (see {@link VrpInstance}).
 *
 * <p>The heuristic is deterministic and runs in O(n²). It is used both as a stand-alone solver and
 * as the initial solution of the Tabu Search and as the reference tour length (C<sub>nn</sub>) of the
 * Ant Colony System.
 */
public final class GreedySolver implements Solver {

    private final VrpInstance instance;

    public GreedySolver(VrpInstance instance) {
        this.instance = instance;
    }

    @Override
    public Solution solve() {
        int n = instance.getDimension();
        boolean[] served = new boolean[n];
        int remaining = instance.getNumberOfCustomers();

        List<List<Integer>> routes = new ArrayList<>();
        while (remaining > 0) {
            List<Integer> route = new ArrayList<>();
            int current = VrpInstance.DEPOT;
            int load = 0;
            double length = 0.0;

            while (true) {
                int next = -1;
                double best = Double.POSITIVE_INFINITY;
                for (int c = 1; c < n; c++) {
                    if (!served[c] && instance.fits(load, c)) {
                        double d = instance.getDistance(current, c);
                        if (d < best && length + d + instance.getDistance(c, VrpInstance.DEPOT) <= instance.getMaxRouteLength()) {
                            best = d;
                            next = c;
                        }
                    }
                }
                if (next == -1) {
                    break; // nothing fits any more: close the route
                }
                route.add(next);
                served[next] = true;
                load += instance.getDemand(next);
                length += best;
                current = next;
                remaining--;
            }
            routes.add(route);
        }
        return Solution.of(instance, routes);
    }
}
