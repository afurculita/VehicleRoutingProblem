package ro.uaic.info.greedy;

import ro.uaic.info.Node;
import ro.uaic.info.VRPLibReader;
import ro.uaic.info.Vehicle;

public class GreedySolver {
    private int noOfVehicles;
    private Node[] nodes;
    private double[][] distances;
    private int noOfCustomers;
    private Vehicle[] vehicles;
    private double cost;

    public GreedySolver(VRPLibReader reader) {
        this.noOfCustomers = reader.getDimension();
        this.noOfVehicles = reader.getDimension();
        this.distances = reader.getDistance();
        this.cost = 0;

        nodes = new Node[noOfCustomers];

        for (int i = 0; i < noOfCustomers; i++) {
            nodes[i] = new Node(i, reader.getDemand()[i]);
        }

        this.vehicles = new Vehicle[this.noOfVehicles];

        for (int i = 0; i < this.noOfVehicles; i++) {
            vehicles[i] = new Vehicle(reader.getVehicleCapacity());
        }
    }

    private boolean unassignedCustomerExists(Node[] Nodes) {
        for (int i = 1; i < Nodes.length; i++) {
            if (!Nodes[i].IsRouted)
                return true;
        }
        return false;
    }

    public void solve() {
        double CandCost, EndCost;
        int VehIndex = 0;

        while (unassignedCustomerExists(nodes)) {
            int CustIndex = 0;
            Node Candidate = null;
            double minCost = (float) Double.MAX_VALUE;

            if (vehicles[VehIndex].routes.isEmpty()) {
                vehicles[VehIndex].AddNode(nodes[0]);
            }

            for (int i = 0; i < noOfCustomers; i++) {
                if (!nodes[i].IsRouted) {
                    if (vehicles[VehIndex].CheckIfFits(nodes[i].demand)) {
                        CandCost = distances[vehicles[VehIndex].currentLocation][i];
                        if (minCost > CandCost) {
                            minCost = CandCost;
                            CustIndex = i;
                            Candidate = nodes[i];
                        }
                    }
                }
            }

            if (Candidate == null) {
                //Not a single Customer Fits
                if (VehIndex + 1 < vehicles.length) //We have more vehicles to assign
                {
                    if (vehicles[VehIndex].currentLocation != 0) {//End this route
                        EndCost = distances[vehicles[VehIndex].currentLocation][0];
                        vehicles[VehIndex].AddNode(nodes[0]);
                        this.cost += EndCost;
                    }
                    VehIndex = VehIndex + 1; //Go to next Vehicle
                } else //We DO NOT have any more vehicle to assign. The problem is unsolved under these parameters
                {
                    System.out.println("\nThe rest customers do not fit in any Vehicle\n" +
                            "The problem cannot be resolved under these constrains");
                    System.exit(0);
                }
            } else {
                vehicles[VehIndex].AddNode(Candidate);//If a fitting Customer is Found
                nodes[CustIndex].IsRouted = true;
                this.cost += minCost;
            }
        }

        EndCost = distances[vehicles[VehIndex].currentLocation][0];
        vehicles[VehIndex].AddNode(nodes[0]);
        this.cost += EndCost;
    }

    public void print() {
        System.out.println("=========================================================");

        for (int j = 0; j < noOfVehicles; j++) {
            if (!vehicles[j].routes.isEmpty()) {
                System.out.print("Vehicle " + j + ":");
                int RoutSize = vehicles[j].routes.size();
                for (int k = 0; k < RoutSize; k++) {
                    if (k == RoutSize - 1) {
                        System.out.print(vehicles[j].routes.get(k).NodeId);
                    } else {
                        System.out.print(vehicles[j].routes.get(k).NodeId + "->");
                    }
                }
                System.out.println();
            }
        }
        System.out.println("\ncost " + this.cost + "\n");
    }

    public Vehicle[] getVehicles() {
        return vehicles;
    }

    public double getCost() {
        return cost;
    }
}


