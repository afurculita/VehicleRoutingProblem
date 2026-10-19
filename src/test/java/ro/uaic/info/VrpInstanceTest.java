package ro.uaic.info;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VrpInstanceTest {

    private static double[][] symmetric() {
        return new double[][] {
                {0, 1, 2},
                {1, 0, 3},
                {2, 3, 0},
        };
    }

    @Test
    void copiesInputArraysDefensively() {
        int[] demand = {0, 1, 2};
        double[][] distance = symmetric();
        VrpInstance instance = new VrpInstance("t", 5, demand, distance);

        demand[1] = 99;
        distance[0][1] = 99;

        assertEquals(1, instance.getDemand(1));
        assertEquals(1.0, instance.getDistance(0, 1));
        instance.getDemands()[1] = 42;
        instance.getDistanceMatrix()[0][1] = 42;
        assertEquals(1, instance.getDemand(1));
        assertEquals(1.0, instance.getDistance(0, 1));
    }

    @Test
    void minimumNumberOfVehiclesIsCeilOfTotalDemandOverCapacity() {
        assertEquals(1, new VrpInstance("t", 5, new int[] {0, 1, 2}, symmetric()).getMinimumNumberOfVehicles());
        assertEquals(2, new VrpInstance("t", 2, new int[] {0, 1, 2}, symmetric()).getMinimumNumberOfVehicles());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 0, new int[] {0, 1, 2}, symmetric()));
    }

    @Test
    void rejectsDepotWithDemand() {
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 5, new int[] {1, 1, 2}, symmetric()));
    }

    @Test
    void rejectsNegativeDemand() {
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 5, new int[] {0, -1, 2}, symmetric()));
    }

    @Test
    void rejectsAsymmetricDistances() {
        double[][] d = symmetric();
        d[1][2] = 4;
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 5, new int[] {0, 1, 2}, d));
    }

    @Test
    void rejectsNonZeroDiagonal() {
        double[][] d = symmetric();
        d[1][1] = 1;
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 5, new int[] {0, 1, 2}, d));
    }

    @Test
    void rejectsDimensionMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 5, new int[] {0, 1}, symmetric()));
    }

    @Test
    void rejectsInstanceWithoutCustomers() {
        assertThrows(IllegalArgumentException.class, () -> new VrpInstance("t", 5, new int[] {0}, new double[][] {{0}}));
    }
}
