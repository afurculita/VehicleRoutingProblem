package vehiclerouting;

import com.ugos.acs.AntGraph;
import vehiclerouting.problem.VehicleRoutingCustomer;
import vehiclerouting.problem.VehicleRoutingProblem;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

public class Solver {
    private static int ANTS = 30;
    private static int ITERATIONS = 250;
    private static int REPETITIONS = 5;
    private static int START_X = 50;
    private static int START_Y = 50;

    public static void main(String[] args) {
        try {
            doRun();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doRun() throws IOException {
        VehicleRoutingProblem r = VehicleRoutingReader.read(new File("datasets/vehiclerouting"));
        ArrayList<VehicleRoutingCustomer> customers = (ArrayList<VehicleRoutingCustomer>) r.getCustomers();
        customers.add(new VehicleRoutingCustomer(customers.size() + 1, START_X, START_Y, r.getVehicleCapacity() * -1));

        double d[][] = new double[customers.size()][customers.size()];
        long[] demands = new long[customers.size()];
        double allDistances = 0;

        for (int i = 0; i < customers.size(); i++) {
            for (int j = i + 1; j < customers.size(); j++) {
                double val = getEuclidDist(customers.get(i).getX(), customers.get(i).getY(), customers.get(j)
                        .getX(), customers.get(j).getY());
                allDistances += val;
                d[i][j] = val;
                d[j][i] = d[i][j];
            }
            d[i][i] = customers.get(i).getDemand();
            demands[i] = customers.get(i).getDemand();
        }

        System.out.println("Customers: " + customers.size() + " \nallDistances: " + allDistances);

        AntGraph graph = new AntGraphVRP(customers.size(), d);

        ObjectOutputStream outs = new ObjectOutputStream(new FileOutputStream("results/t_"
                + customers.size() + "_antgraph.bin"));
        outs.writeObject(graph);
        outs.close();

        FileOutputStream outs1 = new FileOutputStream("results/t_" + customers.size() + "_antgraph.txt");
        for (int i = 0; i < customers.size(); i++) {
            for (int j = 0; j < customers.size(); j++) {
                outs1.write((graph.delta(i, j) + ",").getBytes());
            }
            outs1.write('\n');
        }
        outs1.close();

        PrintStream outs2 = new PrintStream(new FileOutputStream("results/t_" + customers.size() + "x"
                + ANTS + "x" + ITERATIONS + "_results.txt"));

        for (int i = 0; i < REPETITIONS; i++) {
            long t = System.currentTimeMillis();
            graph.resetTau();
            AntColonyVRP antColony = new AntColonyVRP(graph, ANTS, ITERATIONS, r.getVehicleCapacity(), demands);
            antColony.start();
            outs2.println(i + " time " + (System.currentTimeMillis() - t) + ", best_path_value " + antColony.getBestPathValue() + ", weight "
                    + getSum(antColony.getBestPathVector(), demands) + " maxSize " + r.getVehicleCapacity()
                    + ", best_path " + pInt(antColony.getBestPath(), demands) + ", last_b_iter "
                    + antColony.getLastBestPathIteration());
        }
        outs2.close();
    }

    private static double getEuclidDist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private static String pInt(int[] _i, long[] _weights) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int a_i : _i) sb.append(" ").append(_weights[a_i]).append(", ");
        sb.append(" ]");
        return sb.toString();
    }

    private static long getSum(Vector<Integer> _path, long[] _weights) {
        long sum = 0;
        for (Integer a_path : _path) {
            sum += _weights[a_path];
        }
        return sum;
    }
}
