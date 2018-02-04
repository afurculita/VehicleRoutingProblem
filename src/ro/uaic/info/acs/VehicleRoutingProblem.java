package ro.uaic.info.acs;

import ro.uaic.info.VRPLibReader;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VehicleRoutingProblem extends Problem {
    private double Q = 1.0;

    /**
     * Distance Matrix
     */
    private double[][] distance;

    private int[] demands;

    private int numberOfClients;

    private int vehicleCapacity;

    /**
     * Nearest Neighbour heuristic
     */
    private double cnn;

    VehicleRoutingProblem(String filename) throws IOException {
        VRPLibReader r = new VRPLibReader(new InstanceReader(new File(filename)));

        numberOfClients = r.getDimension();
        distance = r.getDistance();
        demands = r.getDemand();
        vehicleCapacity = r.getVehicleCapacity();

        NearestNeighbour nn = new NearestNeighbour();

        this.cnn = evaluate(nn.solve(this));
    }

    @Override
    public double getNij(int i, int j) {
        return 1.0 / distance[i][j];
    }

    @Override
    public boolean better(double s1, double best) {
        return s1 != 0 && best != 0 & s1 < best;
    }

    public double getDistance(int i, int j) {
        return this.distance[i][j];
    }

    @Override
    public double evaluate(int[] solution) {

        double totalDistance = 0;

        for (int h = 1; h < solution.length; h++) {

            int i = solution[h - 1];
            int j = solution[h];

            totalDistance += distance[i][j];
        }

        return totalDistance;
    }

    @Override
    public int getNumberOfNodes() {
        return numberOfClients;
    }

    @Override
    public double getCnn() {
        return cnn;
    }

    @Override
    public double getDeltaTau(double tourLength, int i, int j) {
        return Q / tourLength;
    }

    public int[] getDemands() {
        return demands;
    }

    @Override
    public String toString() {
        return VehicleRoutingProblem.class.getSimpleName();
    }

    @Override
    public List<Integer> initNodesToVisit(int startingNode) {

        List<Integer> nodesToVisit = new ArrayList<>();

        for (Integer i = 0; i < getNumberOfNodes(); i++) {
            if (i != startingNode) {
                nodesToVisit.add(i);
            }
        }

        return nodesToVisit;
    }

    @Override
    public List<Integer> updateNodesToVisit(List<Integer> tour, List<Integer> nodesToVisit) {
        return null;
    }

    public List<Integer> recomputeNodesToVisit(List<Integer> tour, VrpAnt ant) {
        List<Integer> nodesToVisit = new ArrayList<>();

        for (Integer i = 0; i < getNumberOfNodes(); i++) {
            if (i != 0 && !tour.contains(i) && (ant.currentCapacity + demands[i]) <= vehicleCapacity) {
                nodesToVisit.add(i);
            }
        }

        if (nodesToVisit.isEmpty()) {
            if (!tour.get(0).equals(tour.get(tour.size() - 1))) {
                nodesToVisit.add(tour.get(0));
            }
        }

        return nodesToVisit;
    }
}
