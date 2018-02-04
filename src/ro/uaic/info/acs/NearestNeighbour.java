package ro.uaic.info.acs;

import thiagodnf.jacof.util.random.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class NearestNeighbour {
    private JMetalRandom rand = JMetalRandom.getInstance();

    public int[] solve(VehicleRoutingProblem p) {

        List<Integer> citiesToVisit = new ArrayList<>();
        List<Integer> solution = new ArrayList<>();

        int currentCity = rand.nextInt(0, p.getNumberOfNodes() - 1);

        for (int i = 0; i < p.getNumberOfNodes(); i++) {
            if (i != currentCity) {
                citiesToVisit.add(i);
            }
        }

        solution.add(currentCity);

        while (!citiesToVisit.isEmpty()) {

            int nextCity = -1;

            double minDistance = Double.MAX_VALUE;

            for (Integer j : citiesToVisit) {

                double distance = p.getDistance(currentCity, j);

                if (distance < minDistance) {
                    minDistance = distance;
                    nextCity = j;
                }
            }

            solution.add(nextCity);
            citiesToVisit.remove(Integer.valueOf(nextCity));
            currentCity = nextCity;
        }

        //Add the start city in the solution
        solution.add(solution.get(0));

        return solution.stream().mapToInt(x -> x).toArray();
    }
}
