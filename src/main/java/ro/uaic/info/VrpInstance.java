package ro.uaic.info;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable Capacitated Vehicle Routing Problem (CVRP) instance.
 *
 * <p>Nodes are indexed from {@code 0} to {@code dimension - 1}. Node {@code 0} is always the depot;
 * nodes {@code 1 .. dimension - 1} are customers. Distances are symmetric and satisfy
 * {@code distance(i, i) == 0}.
 *
 * <p>Every customer demand is guaranteed to fit into a single vehicle
 * ({@code demand[i] <= capacity}), otherwise the instance would be infeasible.
 */
public final class VrpInstance {

    public static final int DEPOT = 0;

    private final String name;
    private final int capacity;
    private final int[] demand;
    private final double[][] distance;
    private final double maxRouteLength;

    /**
     * @param name     a human-readable instance name (may be empty, never null)
     * @param capacity the vehicle capacity, strictly positive
     * @param demand   demand of each node; the depot (index 0) must have demand 0
     * @param distance a square, symmetric distance matrix with a zero diagonal
     * @throws IllegalArgumentException if the data is inconsistent or the instance is infeasible
     */
    public VrpInstance(String name, int capacity, int[] demand, double[][] distance) {
        this(name, capacity, demand, distance, Double.POSITIVE_INFINITY);
    }

    /**
     * @param maxRouteLength maximum length of a single route ({@code DISTANCE} entry of the Golden and
     *                       Li instances), or {@link Double#POSITIVE_INFINITY} when unconstrained.
     *                       Every solver respects it and {@link Solution#of} validates it.
     */
    public VrpInstance(String name, int capacity, int[] demand, double[][] distance, double maxRouteLength) {
        this.name = Objects.requireNonNull(name, "name");
        this.capacity = capacity;
        this.demand = demand.clone();
        this.distance = deepCopy(distance);
        this.maxRouteLength = maxRouteLength;
        validate();
    }

    private void validate() {
        int n = demand.length;
        if (n < 2) {
            throw new IllegalArgumentException("An instance needs a depot and at least one customer, got dimension " + n);
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Vehicle capacity must be positive, got " + capacity);
        }
        if (Double.isNaN(maxRouteLength) || maxRouteLength <= 0) {
            throw new IllegalArgumentException("Maximum route length must be positive, got " + maxRouteLength);
        }
        if (distance.length != n) {
            throw new IllegalArgumentException("Distance matrix has " + distance.length + " rows but dimension is " + n);
        }
        if (demand[DEPOT] != 0) {
            throw new IllegalArgumentException("The depot must have demand 0, got " + demand[DEPOT]);
        }
        for (int i = 0; i < n; i++) {
            if (distance[i].length != n) {
                throw new IllegalArgumentException("Distance matrix row " + i + " has " + distance[i].length + " columns but dimension is " + n);
            }
            if (demand[i] < 0) {
                throw new IllegalArgumentException("Node " + (i + 1) + " has negative demand " + demand[i]);
            }
            if (demand[i] > capacity) {
                throw new IllegalArgumentException("Node " + (i + 1) + " has demand " + demand[i]
                        + " which exceeds the vehicle capacity " + capacity + "; the instance is infeasible");
            }
            if (i != DEPOT && 2 * distance[DEPOT][i] > maxRouteLength) {
                throw new IllegalArgumentException("Node " + (i + 1) + " cannot be served within the maximum route length "
                        + maxRouteLength + "; the instance is infeasible");
            }
            if (distance[i][i] != 0.0) {
                throw new IllegalArgumentException("distance(" + i + ", " + i + ") must be 0");
            }
            for (int j = i + 1; j < n; j++) {
                if (distance[i][j] < 0 || Double.isNaN(distance[i][j])) {
                    throw new IllegalArgumentException("Invalid distance(" + i + ", " + j + ") = " + distance[i][j]);
                }
                if (distance[i][j] != distance[j][i]) {
                    throw new IllegalArgumentException("Distance matrix is not symmetric at (" + i + ", " + j + ")");
                }
            }
        }
    }

    private static double[][] deepCopy(double[][] m) {
        double[][] copy = new double[m.length][];
        for (int i = 0; i < m.length; i++) {
            copy[i] = m[i].clone();
        }
        return copy;
    }

    public String getName() {
        return name;
    }

    /** Total number of nodes, depot included. */
    public int getDimension() {
        return demand.length;
    }

    /** Number of customers, i.e. {@code dimension - 1}. */
    public int getNumberOfCustomers() {
        return demand.length - 1;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getDemand(int node) {
        return demand[node];
    }

    public double getDistance(int from, int to) {
        return distance[from][to];
    }

    /** A defensive copy of the demand vector. */
    public int[] getDemands() {
        return demand.clone();
    }

    /** A defensive copy of the distance matrix. */
    public double[][] getDistanceMatrix() {
        return deepCopy(distance);
    }

    /** Whether the instance declares a maximum route length. */
    public boolean hasMaxRouteLength() {
        return maxRouteLength != Double.POSITIVE_INFINITY;
    }

    /** Maximum route length declared by the instance, or {@link Double#POSITIVE_INFINITY}. */
    public double getMaxRouteLength() {
        return maxRouteLength;
    }

    /** Total demand of all customers. */
    public int getTotalDemand() {
        return Arrays.stream(demand).sum();
    }

    /** Whether a vehicle can serve the given customers in one route as far as capacity goes. */
    public boolean fits(int load, int customer) {
        return load + demand[customer] <= capacity;
    }

    /** A lower bound on the number of vehicles: {@code ceil(totalDemand / capacity)}. */
    public int getMinimumNumberOfVehicles() {
        return (getTotalDemand() + capacity - 1) / capacity;
    }

    @Override
    public String toString() {
        return "VrpInstance{name='" + name + "', dimension=" + getDimension() + ", capacity=" + capacity + '}';
    }
}
