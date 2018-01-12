package ro.uaic.info.greedy;

import ro.uaic.info.Node;
import ro.uaic.info.Vehicle;

public class GreedySolver {
    private int NoOfVehicles;
    private int NoOfCustomers;
    private Vehicle[] Vehicles;
    private double Cost;

    GreedySolver(int CustNum, int VechNum, int VechCap) {
        this.NoOfVehicles = VechNum;
        this.NoOfCustomers = CustNum;
        this.Cost = 0;
        Vehicles = new Vehicle[NoOfVehicles];

        for (int i = 0; i < NoOfVehicles; i++) {
            Vehicles[i] = new Vehicle(VechCap);
        }
    }

    private boolean UnassignedCustomerExists(Node[] Nodes) {
        for (int i = 1; i < Nodes.length; i++) {
            if (!Nodes[i].IsRouted)
                return true;
        }
        return false;
    }

    public void GreedySolution(Node[] Nodes, double[][] CostMatrix) {

        double CandCost, EndCost;
        int VehIndex = 0;

        while (UnassignedCustomerExists(Nodes)) {

            int CustIndex = 0;
            Node Candidate = null;
            double minCost = (float) Double.MAX_VALUE;

            if (Vehicles[VehIndex].Route.isEmpty()) {
                Vehicles[VehIndex].AddNode(Nodes[0]);
            }

            for (int i = 1; i <= NoOfCustomers; i++) {
                if (!Nodes[i].IsRouted) {
                    if (Vehicles[VehIndex].CheckIfFits(Nodes[i].demand)) {
                        CandCost = CostMatrix[Vehicles[VehIndex].CurLoc][i];
                        if (minCost > CandCost) {
                            minCost = CandCost;
                            CustIndex = i;
                            Candidate = Nodes[i];
                        }
                    }
                }
            }

            if (Candidate == null) {
                //Not a single Customer Fits
                if (VehIndex + 1 < Vehicles.length) //We have more vehicles to assign
                {
                    if (Vehicles[VehIndex].CurLoc != 0) {//End this route
                        EndCost = CostMatrix[Vehicles[VehIndex].CurLoc][0];
                        Vehicles[VehIndex].AddNode(Nodes[0]);
                        this.Cost += EndCost;
                    }
                    VehIndex = VehIndex + 1; //Go to next Vehicle
                } else //We DO NOT have any more vehicle to assign. The problem is unsolved under these parameters
                {
                    System.out.println("\nThe rest customers do not fit in any Vehicle\n" +
                            "The problem cannot be resolved under these constrains");
                    System.exit(0);
                }
            } else {
                Vehicles[VehIndex].AddNode(Candidate);//If a fitting Customer is Found
                Nodes[CustIndex].IsRouted = true;
                this.Cost += minCost;
            }
        }

        EndCost = CostMatrix[Vehicles[VehIndex].CurLoc][0];
        Vehicles[VehIndex].AddNode(Nodes[0]);
        this.Cost += EndCost;
    }

    public void SolutionPrint(String Solution_Label) {
        System.out.println("=========================================================");
        System.out.println(Solution_Label + "\n");

        for (int j = 0; j < NoOfVehicles; j++) {
            if (!Vehicles[j].Route.isEmpty()) {
                System.out.print("Vehicle " + j + ":");
                int RoutSize = Vehicles[j].Route.size();
                for (int k = 0; k < RoutSize; k++) {
                    if (k == RoutSize - 1) {
                        System.out.print(Vehicles[j].Route.get(k).NodeId);
                    } else {
                        System.out.print(Vehicles[j].Route.get(k).NodeId + "->");
                    }
                }
                System.out.println();
            }
        }
        System.out.println("\nCost " + this.Cost + "\n");
    }
}


