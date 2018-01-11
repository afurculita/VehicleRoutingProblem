package vehiclerouting.problem;

import java.util.ArrayList;
import java.util.List;

public class VehicleRoutingProblem {

	private int vehicleNumber;
	private int vehicleCapacity;
	private List<VehicleRoutingCustomer> customers = new ArrayList<VehicleRoutingCustomer>();

	public void init(int vehicleNumber, int vehicleCapacity) {
		this.vehicleNumber = vehicleNumber;
		this.vehicleCapacity = vehicleCapacity;
	}

	public int getVehicleNumber() {
		return vehicleNumber;
	}

	public int getVehicleCapacity() {
		return vehicleCapacity;
	}

	public void additem(VehicleRoutingCustomer c) {
		customers.add(c);

	}

	public List<VehicleRoutingCustomer> getCustomers() {
		return customers;
	}
}
