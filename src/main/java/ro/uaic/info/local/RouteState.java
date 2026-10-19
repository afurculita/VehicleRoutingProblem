package ro.uaic.info.local;

import ro.uaic.info.Solution;
import ro.uaic.info.VrpInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mutable working solution for local search: routes stored with the depot at both ends, per-route
 * loads and lengths, the total cost and a position index of every customer.
 *
 * <p>There is always at least one empty route ({@code [0, 0]}) so that moves can open a new vehicle.
 * Routes are never removed, so a route index identifies the same route for the whole lifetime of
 * the state (which tabu attributes rely on).
 */
public final class RouteState {

    private final VrpInstance instance;
    private final List<List<Integer>> routes = new ArrayList<>();
    private int[] load = new int[0];
    private double[] length = new double[0];
    private double cost;
    private final int[] routeOf;
    private final int[] posOf;

    public RouteState(VrpInstance instance, List<? extends List<Integer>> customerRoutes) {
        this.instance = instance;
        this.routeOf = new int[instance.getDimension()];
        this.posOf = new int[instance.getDimension()];
        reset(customerRoutes);
    }

    public RouteState(VrpInstance instance, Solution solution) {
        this(instance, solution.getRoutes());
    }

    /** Replaces the working solution with the given customer-only routes. */
    public void reset(List<? extends List<Integer>> customerRoutes) {
        routes.clear();
        for (List<Integer> customers : customerRoutes) {
            List<Integer> route = new ArrayList<>(customers.size() + 2);
            route.add(VrpInstance.DEPOT);
            route.addAll(customers);
            route.add(VrpInstance.DEPOT);
            routes.add(route);
        }
        load = new int[routes.size()];
        length = new double[routes.size()];
        for (int r = 0; r < routes.size(); r++) {
            refresh(r);
        }
        ensureSpareRoute();
        recomputeCost();
    }

    public VrpInstance instance() {
        return instance;
    }

    public int numberOfRoutes() {
        return routes.size();
    }

    /** Route {@code r} including the depot at both ends. Must not be modified by callers. */
    public List<Integer> route(int r) {
        return routes.get(r);
    }

    public int load(int r) {
        return load[r];
    }

    public double length(int r) {
        return length[r];
    }

    public double cost() {
        return cost;
    }

    public int routeOf(int customer) {
        return routeOf[customer];
    }

    public int positionOf(int customer) {
        return posOf[customer];
    }

    public int firstEmptyRoute() {
        for (int r = 0; r < routes.size(); r++) {
            if (routes.get(r).size() == 2) {
                return r;
            }
        }
        return -1;
    }

    /** Applies a move generated for the current state and updates all bookkeeping. */
    public void apply(Move move) {
        switch (move) {
            case Move.Relocate m -> {
                List<Integer> source = routes.get(m.from());
                List<Integer> segment = new ArrayList<>(source.subList(m.start(), m.start() + m.length()));
                source.subList(m.start(), m.start() + m.length()).clear();
                if (m.reversed()) {
                    Collections.reverse(segment);
                }
                int insertAt = m.after() + 1;
                if (m.sameRoute() && m.after() > m.start()) {
                    insertAt -= m.length();
                }
                routes.get(m.to()).addAll(insertAt, segment);
                refresh(m.from());
                if (!m.sameRoute()) {
                    refresh(m.to());
                }
            }
            case Move.Swap m -> {
                routes.get(m.route1()).set(m.index1(), m.customer2());
                routes.get(m.route2()).set(m.index2(), m.customer1());
                refresh(m.route1());
                if (!m.sameRoute()) {
                    refresh(m.route2());
                }
            }
            case Move.TwoOpt m -> {
                Collections.reverse(routes.get(m.route()).subList(m.i(), m.j() + 1));
                refresh(m.route());
            }
            case Move.CrossExchange m -> {
                List<Integer> route1 = routes.get(m.route1());
                List<Integer> route2 = routes.get(m.route2());
                List<Integer> tail1 = new ArrayList<>(route1.subList(m.index1() + 1, route1.size()));
                List<Integer> tail2 = new ArrayList<>(route2.subList(m.index2() + 1, route2.size()));
                route1.subList(m.index1() + 1, route1.size()).clear();
                route2.subList(m.index2() + 1, route2.size()).clear();
                route1.addAll(tail2);
                route2.addAll(tail1);
                refresh(m.route1());
                refresh(m.route2());
            }
        }
        ensureSpareRoute();
        recomputeCost();
    }

    /** The routes without depots and without empty routes. */
    public List<List<Integer>> toCustomerRoutes() {
        List<List<Integer>> result = new ArrayList<>();
        for (List<Integer> route : routes) {
            if (route.size() > 2) {
                result.add(new ArrayList<>(route.subList(1, route.size() - 1)));
            }
        }
        return result;
    }

    public Solution toSolution() {
        return Solution.of(instance, toCustomerRoutes());
    }

    private void ensureSpareRoute() {
        if (firstEmptyRoute() == -1) {
            routes.add(new ArrayList<>(List.of(VrpInstance.DEPOT, VrpInstance.DEPOT)));
            load = java.util.Arrays.copyOf(load, routes.size());
            length = java.util.Arrays.copyOf(length, routes.size());
        }
    }

    private void refresh(int r) {
        List<Integer> route = routes.get(r);
        int l = 0;
        double len = 0.0;
        for (int p = 1; p < route.size(); p++) {
            int node = route.get(p);
            len += instance.getDistance(route.get(p - 1), node);
            if (node != VrpInstance.DEPOT) {
                l += instance.getDemand(node);
                routeOf[node] = r;
                posOf[node] = p;
            }
        }
        load[r] = l;
        length[r] = len;
    }

    private void recomputeCost() {
        double total = 0.0;
        for (double len : length) {
            total += len;
        }
        cost = total;
    }
}
