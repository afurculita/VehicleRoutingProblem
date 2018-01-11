package vehiclerouting.problem;

public class VehicleRoutingCustomer {

	private double x;
	private double y;
	private int demand;
	private int id;

	public VehicleRoutingCustomer(int id, double x, double y, int demand) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.demand = demand;

	}

	public int getId() {
		return id;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public int getDemand() {
		return demand;
	}
}
