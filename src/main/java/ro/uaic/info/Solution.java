package ro.uaic.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A set of vehicle routes for a {@link VrpInstance}, together with its total cost.
 *
 * <p>Each route lists the customers in visiting order; the depot (node 0) is implicit at both
 * ends. The cost is the sum, over all routes, of the distances depot → c1 → … → ck → depot.
 *
 * <p>Instances are immutable. {@link #of(VrpInstance, List)} computes the cost and checks
 * feasibility (every customer served exactly once, no route over capacity or over the maximum
 * route length when the instance declares one).
 */
public final class Solution {

    /** Tolerance used when comparing costs. */
    public static final double EPSILON = 1e-9;

    private final List<List<Integer>> routes;
    private final double cost;

    private Solution(List<List<Integer>> routes, double cost) {
        this.routes = routes;
        this.cost = cost;
    }

    /**
     * Builds a solution from customer-only routes, dropping empty routes.
     *
     * @throws IllegalArgumentException if the routes are not a feasible solution of {@code instance}
     */
    public static Solution of(VrpInstance instance, List<? extends List<Integer>> routes) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> route : routes) {
            if (!route.isEmpty()) {
                copy.add(List.copyOf(route));
            }
        }
        List<List<Integer>> frozen = List.copyOf(copy);
        checkFeasible(instance, frozen);
        return new Solution(frozen, cost(instance, frozen));
    }

    /** Sum of the route lengths of {@code routes} on {@code instance}, depot legs included. */
    public static double cost(VrpInstance instance, List<? extends List<Integer>> routes) {
        double total = 0.0;
        for (List<Integer> route : routes) {
            total += routeCost(instance, route);
        }
        return total;
    }

    /** Length of the closed route depot → route → depot. Empty routes cost 0. */
    public static double routeCost(VrpInstance instance, List<Integer> route) {
        if (route.isEmpty()) {
            return 0.0;
        }
        double total = instance.getDistance(VrpInstance.DEPOT, route.get(0));
        for (int k = 1; k < route.size(); k++) {
            total += instance.getDistance(route.get(k - 1), route.get(k));
        }
        total += instance.getDistance(route.get(route.size() - 1), VrpInstance.DEPOT);
        return total;
    }

    private static void checkFeasible(VrpInstance instance, List<List<Integer>> routes) {
        int n = instance.getDimension();
        boolean[] served = new boolean[n];
        for (int r = 0; r < routes.size(); r++) {
            int load = 0;
            for (int customer : routes.get(r)) {
                if (customer <= VrpInstance.DEPOT || customer >= n) {
                    throw new IllegalArgumentException("Route " + r + " contains invalid node " + customer);
                }
                if (served[customer]) {
                    throw new IllegalArgumentException("Customer " + customer + " is served more than once");
                }
                served[customer] = true;
                load += instance.getDemand(customer);
            }
            if (load > instance.getCapacity()) {
                throw new IllegalArgumentException("Route " + r + " has load " + load
                        + " which exceeds the capacity " + instance.getCapacity());
            }
            double length = routeCost(instance, routes.get(r));
            if (length > instance.getMaxRouteLength() + EPSILON) {
                throw new IllegalArgumentException("Route " + r + " has length " + length
                        + " which exceeds the maximum route length " + instance.getMaxRouteLength());
            }
        }
        for (int c = 1; c < n; c++) {
            if (!served[c]) {
                throw new IllegalArgumentException("Customer " + c + " is not served");
            }
        }
    }

    /** The routes, each an unmodifiable list of customers (depot excluded). Never contains empty routes. */
    public List<List<Integer>> getRoutes() {
        return routes;
    }

    public int getNumberOfRoutes() {
        return routes.size();
    }

    public double getCost() {
        return cost;
    }

    /** Load of route {@code r} on the given instance. */
    public int getLoad(VrpInstance instance, int r) {
        int load = 0;
        for (int customer : routes.get(r)) {
            load += instance.getDemand(customer);
        }
        return load;
    }

    /**
     * Human-readable rendering, e.g.
     * <pre>
     * Vehicle 0: 0 -> 5 -> 3 -> 0
     * Vehicle 1: 0 -> 1 -> 0
     *
     * Best Value: 123.456
     * </pre>
     * The {@code Best Value:} line is parsed by the irace tuning scripts, keep its format stable.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < routes.size(); r++) {
            sb.append("Vehicle ").append(r).append(": ").append(VrpInstance.DEPOT);
            for (int customer : routes.get(r)) {
                sb.append(" -> ").append(customer);
            }
            sb.append(" -> ").append(VrpInstance.DEPOT).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        sb.append(String.format(Locale.ROOT, "Best Value: %.4f", cost)).append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Solution{routes=" + routes.size() + ", cost=" + cost + '}';
    }
}
