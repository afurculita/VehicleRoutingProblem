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
    public double Q = 1.0;

    /** Distance Matrix */
    private double[][] distance;

    /** Number of Cities */
    private int numberOfCities;

    /** Nearest Neighbour heuristic */
    private double cnn;

    public VRPProblem(String filename) throws IOException {

        VRPLibReader r = new VRPLibReader(new InstanceReader(new File(filename)));

        numberOfCities = r.getDimension();

        distance = r.getDistance();

        NearestNeighbour nn = new NearestNeighbour();

        this.cnn = evaluate(nn.solve(this));

        System.out.println("Best Solution: " + Arrays.toString(getTheBestSolution()));
        System.out.println("Best Value: " + evaluate(getTheBestSolution()));
    }

    @Override
    public double getNij(int i, int j) {
        return 1.0 / distance[i][j];
    }

    @Override
    public boolean better(double s1, double best) {
        return s1 < best;
    }

    public double getDistance(int i, int j) {
        return this.distance[i][j];
    }

    public int[] getTheBestSolution(){
        return new int[]{0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,24,23,25,26,27,28,29,1, 0};
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
        return numberOfCities;
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

        // Add all nodes (or cities) less the start node
        for (int i = 0; i < getNumberOfNodes(); i++) {
            if (i != startingNode) {
                nodesToVisit.add(i);
            }
        }

        return nodesToVisit;
    }

    @Override
    public List<Integer> updateNodesToVisit(List<Integer> tour, List<Integer> nodesToVisit) {

        if (nodesToVisit.isEmpty()) {
            if (!tour.get(0).equals(tour.get(tour.size() - 1))) {
                nodesToVisit.add(tour.get(0));
            }
        }

        return nodesToVisit;
    }
}
