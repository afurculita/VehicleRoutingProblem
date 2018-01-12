package ro.uaic.info;

import java.util.ArrayList;

public class Vehicle {
    public ArrayList<Node> Route = new ArrayList<>();
    private int capacity;
    public int load;
    public int CurLoc;

    public Vehicle(int cap) {
        this.capacity = cap;
        this.load = 0;
        this.CurLoc = 0; //In depot Initially
        this.Route.clear();
    }

    public void AddNode(Node Customer)//Add Customer to Vehicle Route
    {
        Route.add(Customer);
        this.load += Customer.demand;
        this.CurLoc = Customer.NodeId;
    }

    public boolean CheckIfFits(int dem) //Check if we have Capacity Violation
    {
        return ((load + dem <= capacity));
    }
}