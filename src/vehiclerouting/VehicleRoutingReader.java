package vehiclerouting;

import vehiclerouting.problem.VehicleRoutingCustomer;
import vehiclerouting.problem.VehicleRoutingProblem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class VehicleRoutingReader {
	public static VehicleRoutingProblem read(File f) throws IOException {
		VehicleRoutingProblem r = new VehicleRoutingProblem();
		Properties p = new Properties();
		p.load(new FileInputStream(f));

		String vehicleNumber = (String) p.get("vehicle_number");
		String vehicleCapacity = (String) p.get("vehicle_capacity");
		r.init(Integer.parseInt(vehicleNumber), Integer.parseInt(vehicleCapacity));

		String[] customers = p.getProperty("customers").split(",");
		String[] demands = p.getProperty("demands").split(",");
		String[] coord;
		for (int i = 0; i < customers.length; i++) {
			coord = customers[i].split("/");
			VehicleRoutingCustomer item = new VehicleRoutingCustomer(i, Integer.parseInt(coord[0]),
					Integer.parseInt(coord[1]), Integer.parseInt(demands[i]));
			r.additem(item);
		}

		return r;

	}
}
