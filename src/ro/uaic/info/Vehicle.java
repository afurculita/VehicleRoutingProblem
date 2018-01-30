package ro.uaic.info;

import java.util.ArrayList;

public class Vehicle {
    public ArrayList<Node> routes = new ArrayList<>();
    private int capacity;
    public int load;
    public int currentLocation;

    public Vehicle(int cap) {
        this.capacity = cap;
        this.load = 0;
        this.currentLocation = 0; //In depot Initially
        this.routes.clear();
    }

    public void AddNode(Node Customer)//Add Customer to Vehicle routes
    {
        routes.add(Customer);
        this.load += Customer.demand;
        this.currentLocation = Customer.NodeId;
    }

    public boolean CheckIfFits(int dem) //Check if we have Capacity Violation
    {
        return load + dem <= capacity;
    }
}