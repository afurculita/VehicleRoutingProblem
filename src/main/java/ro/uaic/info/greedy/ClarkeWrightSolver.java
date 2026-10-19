package ro.uaic.info.greedy;

import ro.uaic.info.Solution;
import ro.uaic.info.Solver;
import ro.uaic.info.VrpInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clarke &amp; Wright (1964) parallel savings heuristic.
 *
 * <p>Every customer starts in its own route. Merging the routes ending in customer {@code i} and
 * starting in customer {@code j} saves {@code s(i, j) = d(0, i) + d(0, j) - d(i, j)}. Savings are
 * processed in decreasing order and a merge is performed whenever both customers are at the end of
 * their routes, the routes differ and the merged route respects the capacity and the maximum route
 * length. Routes are reversed when necessary so that the two customers become adjacent.
 *
 * <p>Runs in O(n² log n) and gives a much better starting point than nearest neighbour, especially on
 * large instances.
 */
public final class ClarkeWrightSolver implements Solver {

    private final VrpInstance instance;

    public ClarkeWrightSolver(VrpInstance instance) {
        this.instance = instance;
    }

    private record Saving(int i, int j, double value) {
    }

    @Override
    public Solution solve() {
        int n = instance.getDimension();
        List<List<Integer>> routes = new ArrayList<>();
        int[] routeOf = new int[n];
        int[] load = new int[n];
        double[] length = new double[n];
        for (int c = 1; c < n; c++) {
            routes.add(new ArrayList<>(List.of(c)));
            routeOf[c] = c - 1;
            load[c - 1] = instance.getDemand(c);
            length[c - 1] = 2 * instance.getDistance(VrpInstance.DEPOT, c);
        }

        List<Saving> savings = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (instance.getDemand(i) + instance.getDemand(j) <= instance.getCapacity()) {
                    double value = instance.getDistance(VrpInstance.DEPOT, i) + instance.getDistance(VrpInstance.DEPOT, j)
                            - instance.getDistance(i, j);
                    if (value > 0) {
                        savings.add(new Saving(i, j, value));
                    }
                }
            }
        }
        savings.sort((a, b) -> Double.compare(b.value(), a.value()));

        for (Saving saving : savings) {
            int ri = routeOf[saving.i()];
            int rj = routeOf[saving.j()];
            if (ri == rj || load[ri] + load[rj] > instance.getCapacity()) {
                continue;
            }
            List<Integer> routeI = routes.get(ri);
            List<Integer> routeJ = routes.get(rj);
            boolean iFirst = routeI.get(0) == saving.i();
            boolean iLast = routeI.get(routeI.size() - 1) == saving.i();
            boolean jFirst = routeJ.get(0) == saving.j();
            boolean jLast = routeJ.get(routeJ.size() - 1) == saving.j();
            if (!(iFirst || iLast) || !(jFirst || jLast)) {
                continue; // an interior customer cannot be joined
            }
            double merged = length[ri] + length[rj] - saving.value();
            if (merged > instance.getMaxRouteLength()) {
                continue;
            }
            // Orient both routes so that i ends routeI and j starts routeJ, then concatenate.
            if (!iLast) {
                Collections.reverse(routeI);
            }
            if (!jFirst) {
                Collections.reverse(routeJ);
            }
            routeI.addAll(routeJ);
            for (int customer : routeJ) {
                routeOf[customer] = ri;
            }
            load[ri] += load[rj];
            length[ri] = merged;
            routeJ.clear();
            load[rj] = 0;
            length[rj] = 0.0;
        }

        List<List<Integer>> result = new ArrayList<>();
        for (List<Integer> route : routes) {
            if (!route.isEmpty()) {
                result.add(route);
            }
        }
        return Solution.of(instance, result);
    }

    @Override
    public String toString() {
        return "ClarkeWright";
    }
}
