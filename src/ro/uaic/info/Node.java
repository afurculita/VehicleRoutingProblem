package ro.uaic.info;

public class Node {
    public int NodeId;
    public int demand; //Node Demand if Customer
    public boolean IsRouted;

    public Node(int id, int demand) //Cunstructor for Customers
    {
        this.NodeId = id;
        this.demand = demand;
        this.IsRouted = false;
    }
}