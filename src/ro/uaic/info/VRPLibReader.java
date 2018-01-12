package ro.uaic.info;

import thiagodnf.jacof.util.io.InstanceReader;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class VRPLibReader {

    private InstanceReader reader;

    private int dimension;
    private int vehicleCapacity;
    private int totalCapacity;

    private double[][] coord;
    private double[][] distance;
    private double[][] demand;
    private double[][] pickup;
    private LocalTime[][] timeWindows;
    private int[] standTime;
    private int[] depots;

    public VRPLibReader(InstanceReader reader) {
        this.reader = reader;

        readHeader();
        readCoordinates();
        readDemand();
        readPickup();
        readTimeWindows();
        readStandtime();
        readDepots();
        convertCoordToDistance();
    }

    private void readHeader() {
        String line = reader.readLine();

        while (!line.equalsIgnoreCase("NODE_COORD_SECTION")) {
            String[] split = line.split(":");

            String key = split[0].trim();

            if (key.equalsIgnoreCase("DIMENSION")) {
                dimension = Integer.valueOf(split[1].trim());
            }

            if (key.equalsIgnoreCase("CAPACITY")) {
                totalCapacity = Integer.valueOf(split[1].trim());
            }

            if (key.equalsIgnoreCase("CAPACITY_VOL")) {
                vehicleCapacity = Integer.valueOf(split[1].trim());
            }

            line = reader.readLine();

            if (line == null) {
                break;
            }
        }
    }

    private void readCoordinates() {
        coord = new double[dimension][2];

        String line = reader.readLine();
        while (!line.equalsIgnoreCase("DEMAND_SECTION")) {
            String[] split = line.split(" ");

            int i = Integer.valueOf(split[0].trim()) - 1;
            coord[i][0] = Double.valueOf(split[1].trim());
            coord[i][1] = Double.valueOf(split[2].trim());

            line = reader.readLine();
        }
    }

    private void readDemand() {
        demand = new double[dimension][2];

        String line = reader.readLine();
        while (!line.equalsIgnoreCase("PICKUP_SECTION")) {
            String[] split = line.split(" ");

            int i = Integer.valueOf(split[0].trim()) - 1;
            demand[i][0] = Double.valueOf(split[1].trim());
            demand[i][1] = Double.valueOf(split[2].trim());

            line = reader.readLine();
        }
    }

    private void readPickup() {
        pickup = new double[dimension][2];

        String line = reader.readLine();
        while (!line.equalsIgnoreCase("TIME_WINDOW_SECTION")) {
            String[] split = line.split(" ");

            int i = Integer.valueOf(split[0].trim()) - 1;
            pickup[i][0] = Double.valueOf(split[1].trim());
            pickup[i][1] = Double.valueOf(split[2].trim());

            line = reader.readLine();
        }
    }

    private void readTimeWindows() {
        timeWindows = new LocalTime[dimension][2];
        DateTimeFormatter f2 = DateTimeFormatter.ofPattern("hh:mm");

        String line = reader.readLine();
        while (!line.equalsIgnoreCase("STANDTIME_SECTION")) {
            String[] split = line.split(" ");

            int i = Integer.valueOf(split[0].trim()) - 1;

            String startTime = split[1].trim();
            String endTime = split[2].trim();
            if (startTime.equals("")) {
                startTime = "0" + split[2].trim();
                endTime = split[3].trim();

                if (endTime.equals("")) {
                    endTime = "0" + split[4].trim();
                }
            }

            timeWindows[i][0] = LocalTime.parse(startTime);
            timeWindows[i][1] = LocalTime.parse(endTime);

            line = reader.readLine();
        }
    }

    private void readStandtime() {
        standTime = new int[dimension];

        String line = reader.readLine();
        while (!line.equalsIgnoreCase("DEPOT_SECTION")) {
            String[] split = line.split(" ");

            int i = Integer.valueOf(split[0].trim()) - 1;
            standTime[i] = Integer.valueOf(split[1].trim());

            line = reader.readLine();
        }
    }

    private void readDepots() {
        depots = new int[2];

        String line = reader.readLine();
        int i = 0;
        while (!line.equalsIgnoreCase("EOF")) {
            depots[i] = Integer.valueOf(line.trim());
            i++;

            line = reader.readLine();
        }
    }

    private void convertCoordToDistance() {
        distance = new double[dimension][dimension];

        for (int i = 0; i < dimension; i++) {
            for (int j = i; j < dimension; j++) {
                if (i != j) {
                    double x1 = coord[i][0];
                    double y1 = coord[i][1];
                    double x2 = coord[j][0];
                    double y2 = coord[j][1];

                    distance[i][j] = euclideanDistance(x1, y1, x2, y2);
                    distance[j][i] = distance[i][j];
                }
            }
        }
    }

    public static double euclideanDistance(double x1, double y1, double x2, double y2) {
        double xDistance = Math.abs(x1 - x2);
        double yDistance = Math.abs(y1 - y2);

        return Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2));
    }

    public int getDimension() {
        return dimension;
    }

    public double[][] getDistance() {
        return distance;
    }

    public int getVehicleCapacity() {
        return vehicleCapacity;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public double[][] getDemand() {
        return demand;
    }

    public double[][] getPickup() {
        return pickup;
    }

    public LocalTime[][] getTimeWindows() {
        return timeWindows;
    }

    public int[] getStandTime() {
        return standTime;
    }

    public int[] getDepots() {
        return depots;
    }
}
