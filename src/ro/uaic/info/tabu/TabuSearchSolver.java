package ro.uaic.info.tabu;

import ro.uaic.info.Node;
import ro.uaic.info.VRPLibReader;
import ro.uaic.info.Vehicle;
import ro.uaic.info.greedy.GreedySolver;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

public class TabuSearchSolver {
    private final double[][] distances;
    private final int noOfVehicles;
    private final int TABU_Horizon;
    private Vehicle[] vehicles;
    private double cost;
    private static final int MAX_ITERATIONS = 10000;

    private final Vehicle[] BestSolutionVehicles;
    private double BestSolutionCost;

    public TabuSearchSolver(VRPLibReader reader, int TABU_Horizon) {
        this.noOfVehicles = reader.getDimension();
        this.TABU_Horizon = TABU_Horizon;
        this.distances = reader.getDistance();

        GreedySolver greedySolver = new GreedySolver(reader);
        greedySolver.solve();
        this.vehicles = greedySolver.getVehicles();
        this.cost = greedySolver.getCost();

        this.BestSolutionVehicles = new Vehicle[this.noOfVehicles];

        for (int i = 0; i < this.noOfVehicles; i++) {
            this.BestSolutionVehicles[i] = new Vehicle(reader.getVehicleCapacity());
        }
    }

    public void solve() {
        //We use 1-0 exchange move
        ArrayList<Node> routesFrom;
        ArrayList<Node> routesTo;

        int MovingNodeDemand = 0;

        int VehIndexFrom, VehIndexTo;
        double BestNCost, NeighborCost;

        int SwapIndexA = -1, SwapIndexB = -1, SwapRouteFrom = -1, SwapRouteTo = -1;
        int iteration_number = 0;

        int DimensionCustomer = this.distances[1].length;
        int TABU_Matrix[][] = new int[DimensionCustomer + 1][DimensionCustomer + 1];

        this.BestSolutionCost = this.cost;

        while (true) {
            BestNCost = Double.MAX_VALUE;

            for (VehIndexFrom = 0; VehIndexFrom < this.vehicles.length; VehIndexFrom++) {
                routesFrom = this.vehicles[VehIndexFrom].routes;
                int RoutFromLength = routesFrom.size();

                for (int i = 1; i < (RoutFromLength - 1); i++) { //Not possible to move depot!
                    for (VehIndexTo = 0; VehIndexTo < this.vehicles.length; VehIndexTo++) {
                        routesTo = this.vehicles[VehIndexTo].routes;
                        int RouteToLength = routesTo.size();
                        for (int j = 0; (j < RouteToLength - 1); j++) {//Not possible to move after last Depot!

                            MovingNodeDemand = routesFrom.get(i).demand;

                            if ((VehIndexFrom == VehIndexTo) || this.vehicles[VehIndexTo].CheckIfFits(MovingNodeDemand)) {
                                //If we assign to a different route check capacity constrains
                                //if in the new route is the same no need to check for capacity

                                if (!((VehIndexFrom == VehIndexTo) && ((j == i) || (j == i - 1))))  // Not a move that Changes solution cost
                                {
                                    double MinusCost1 = this.distances[routesFrom.get(i - 1).NodeId][routesFrom.get(i).NodeId];
                                    double MinusCost2 = this.distances[routesFrom.get(i).NodeId][routesFrom.get(i + 1).NodeId];
                                    double MinusCost3 = this.distances[routesTo.get(j).NodeId][routesTo.get(j + 1).NodeId];

                                    double AddedCost1 = this.distances[routesFrom.get(i - 1).NodeId][routesFrom.get(i + 1).NodeId];
                                    double AddedCost2 = this.distances[routesTo.get(j).NodeId][routesFrom.get(i).NodeId];
                                    double AddedCost3 = this.distances[routesFrom.get(i).NodeId][routesTo.get(j + 1).NodeId];

                                    //Check if the move is a Tabu! - If it is Tabu break
                                    if ((TABU_Matrix[routesFrom.get(i - 1).NodeId][routesFrom.get(i + 1).NodeId] != 0)
                                            || (TABU_Matrix[routesTo.get(j).NodeId][routesFrom.get(i).NodeId] != 0)
                                            || (TABU_Matrix[routesFrom.get(i).NodeId][routesTo.get(j + 1).NodeId] != 0)) {
                                        break;
                                    }

                                    NeighborCost = AddedCost1 + AddedCost2 + AddedCost3
                                            - MinusCost1 - MinusCost2 - MinusCost3;

                                    if (NeighborCost < BestNCost) {
                                        BestNCost = NeighborCost;
                                        SwapIndexA = i;
                                        SwapIndexB = j;
                                        SwapRouteFrom = VehIndexFrom;
                                        SwapRouteTo = VehIndexTo;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (int o = 0; o < TABU_Matrix[0].length; o++) {
                for (int p = 0; p < TABU_Matrix[0].length; p++) {
                    if (TABU_Matrix[o][p] > 0) {
                        TABU_Matrix[o][p]--;
                    }
                }
            }

            routesFrom = this.vehicles[SwapRouteFrom].routes;
            routesTo = this.vehicles[SwapRouteTo].routes;
            this.vehicles[SwapRouteFrom].routes = null;
            this.vehicles[SwapRouteTo].routes = null;

            Node SwapNode = routesFrom.get(SwapIndexA);

            int NodeIDBefore = routesFrom.get(SwapIndexA - 1).NodeId;
            int NodeIDAfter = routesFrom.get(SwapIndexA + 1).NodeId;
            int NodeID_F = routesTo.get(SwapIndexB).NodeId;
            int NodeID_G = routesTo.get(SwapIndexB + 1).NodeId;

            Random TabuRan = new Random();
            int randomDelay1 = TabuRan.nextInt(5);
            int randomDelay2 = TabuRan.nextInt(5);
            int randomDelay3 = TabuRan.nextInt(5);

            TABU_Matrix[NodeIDBefore][SwapNode.NodeId] = this.TABU_Horizon + randomDelay1;
            TABU_Matrix[SwapNode.NodeId][NodeIDAfter] = this.TABU_Horizon + randomDelay2;
            TABU_Matrix[NodeID_F][NodeID_G] = this.TABU_Horizon + randomDelay3;

            routesFrom.remove(SwapIndexA);

            if (SwapRouteFrom == SwapRouteTo) {
                if (SwapIndexA < SwapIndexB) {
                    routesTo.add(SwapIndexB, SwapNode);
                } else {
                    routesTo.add(SwapIndexB + 1, SwapNode);
                }
            } else {
                routesTo.add(SwapIndexB + 1, SwapNode);
            }

            this.vehicles[SwapRouteFrom].routes = routesFrom;
            this.vehicles[SwapRouteFrom].load -= MovingNodeDemand;

            this.vehicles[SwapRouteTo].routes = routesTo;
            this.vehicles[SwapRouteTo].load += MovingNodeDemand;

            this.cost += BestNCost;

            if (this.cost < this.BestSolutionCost) {
                iteration_number = 0;
                this.SaveBestSolution();
            } else {
                iteration_number++;
            }

            if (MAX_ITERATIONS == iteration_number) {
                break;
            }
        }

        this.vehicles = this.BestSolutionVehicles;
        this.cost = this.BestSolutionCost;
    }

    private void SaveBestSolution() {
        this.BestSolutionCost = this.cost;
        for (int j = 0; j < this.noOfVehicles; j++) {
            this.BestSolutionVehicles[j].routes.clear();
            if (!this.vehicles[j].routes.isEmpty()) {
                int RoutSize = this.vehicles[j].routes.size();
                for (int k = 0; k < RoutSize; k++) {
                    Node n = this.vehicles[j].routes.get(k);
                    this.BestSolutionVehicles[j].routes.add(n);
                }
            }
        }
    }

    public void print() {
        System.out.println("=========================================================");

        for (int j = 0; j < this.noOfVehicles; j++) {
            if (!this.vehicles[j].routes.isEmpty()) {
                System.out.print("Vehicle " + j + ":");
                int RoutSize = this.vehicles[j].routes.size();
                for (int k = 0; k < RoutSize; k++) {
                    if (k == RoutSize - 1) {
                        System.out.print(this.vehicles[j].routes.get(k).NodeId);
                    } else {
                        System.out.print(this.vehicles[j].routes.get(k).NodeId + "->");
                    }
                }
                System.out.println();
            }
        }
        System.out.println("\nTabuSearchSolver cost " + this.cost + "\n");
    }
}


