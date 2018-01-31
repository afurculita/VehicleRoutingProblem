package ro.uaic.info.acs;

import ro.uaic.info.VRPLibReader;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VRPProblem extends Problem {
    private double Q = 1.0;

    /**
     * Distance Matrix
     */
    private double[][] distance;

    private int[] demands;

    private int numberOfClients;

    private int vehicleCapacity;

    private List<Integer> visitedNodes;

    /**
     * Nearest Neighbour heuristic
     */
    private double cnn;

    VRPProblem(String filename) throws IOException {

        VRPLibReader r = new VRPLibReader(new InstanceReader(new File(filename)));

        numberOfClients = r.getDimension();
        distance = r.getDistance();
        demands = r.getDemand();
        vehicleCapacity = r.getVehicleCapacity();

        visitedNodes = new ArrayList<>();

        NearestNeighbour nn = new NearestNeighbour();

        this.cnn = evaluate(nn.solve(this));
    }

    public void reset() {
        visitedNodes = new ArrayList<>();
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

    @Override
    public String toString() {
        return VRPProblem.class.getSimpleName();
    }

    @Override
    public List<Integer> initNodesToVisit(int startingNode) {

        List<Integer> nodesToVisit = new ArrayList<>();

        for (Integer i = 0; i < getNumberOfNodes(); i++) {
            if (i != startingNode && demands[i] <= vehicleCapacity && !visitedNodes.contains(i)) {
                nodesToVisit.add(i);
            }
        }

        return nodesToVisit;
    }

    @Override
    public List<Integer> updateNodesToVisit(List<Integer> tour, List<Integer> nodesToVisit) {
        List<Integer> nodesToRemove = new ArrayList<>();

        double totalCost = 0.0;

        for (Integer i : tour) {
            if (!visitedNodes.contains(i)) {
                visitedNodes.add(i);
            }

            totalCost += demands[i];
        }

        for (Integer i : nodesToVisit) {
            if (totalCost + demands[i] > vehicleCapacity) {
                nodesToRemove.add(i);
            }
        }

        for (Integer i : nodesToRemove) {
            nodesToVisit.remove(i);
        }

        if (nodesToVisit.isEmpty()) {
            if (!tour.get(0).equals(tour.get(tour.size() - 1))) {
                nodesToVisit.add(tour.get(0));
            }
        }

        return nodesToVisit;
    }
}
