package ro.uaic.info.local;

import ro.uaic.info.Solution;
import ro.uaic.info.VrpInstance;

import java.util.Collections;
import java.util.List;

/**
 * Intra-route 2-opt local search.
 *
 * <p>A 2-opt move removes two arcs of a route and reconnects it by reversing the segment in between.
 * Because distances are symmetric only the two replaced arcs change the route cost. Capacity is
 * unaffected since the set of customers of the route does not change.
 */
public final class TwoOpt {

    private TwoOpt() {
    }

    /**
     * Repeatedly applies the best improving 2-opt move to {@code route} (customers only, modified in
     * place) until the route is 2-optimal.
     *
     * @return the total cost reduction achieved (>= 0)
     */
    public static double improve(VrpInstance instance, List<Integer> route) {
        double totalGain = 0.0;
        while (true) {
            double bestDelta = -Solution.EPSILON;
            int bestI = -1;
            int bestJ = -1;
            int size = route.size();
            for (int i = 0; i < size - 1; i++) {
                int prev = i == 0 ? VrpInstance.DEPOT : route.get(i - 1);
                int ci = route.get(i);
                for (int j = i + 1; j < size; j++) {
                    int cj = route.get(j);
                    int next = j == size - 1 ? VrpInstance.DEPOT : route.get(j + 1);
                    double delta = instance.getDistance(prev, cj) + instance.getDistance(ci, next)
                            - instance.getDistance(prev, ci) - instance.getDistance(cj, next);
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            if (bestI < 0) {
                return totalGain;
            }
            Collections.reverse(route.subList(bestI, bestJ + 1));
            totalGain -= bestDelta;
        }
    }

    /** Applies {@link #improve(VrpInstance, List)} to every route. */
    public static double improveAll(VrpInstance instance, List<? extends List<Integer>> routes) {
        double totalGain = 0.0;
        for (List<Integer> route : routes) {
            totalGain += improve(instance, route);
        }
        return totalGain;
    }
}
