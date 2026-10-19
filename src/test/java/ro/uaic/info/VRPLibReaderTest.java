package ro.uaic.info;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VRPLibReaderTest {

    @Test
    void parsesHeaderCoordinatesAndDemands() {
        VrpInstance instance = TestInstances.tiny();

        assertEquals("tiny", instance.getName());
        assertEquals(5, instance.getDimension());
        assertEquals(4, instance.getNumberOfCustomers());
        assertEquals(10, instance.getCapacity());
        assertEquals(0, instance.getDemand(0));
        assertEquals(6, instance.getDemand(4));
        assertEquals(21, instance.getTotalDemand());
        assertEquals(3, instance.getMinimumNumberOfVehicles());
    }

    @Test
    void computesSymmetricEuclideanDistances() {
        VrpInstance instance = TestInstances.tiny();

        assertEquals(3.0, instance.getDistance(0, 1), 1e-12);
        assertEquals(4.0, instance.getDistance(0, 2), 1e-12);
        assertEquals(5.0, instance.getDistance(1, 2), 1e-12);
        assertEquals(5.0, instance.getDistance(2, 1), 1e-12);
        assertEquals(8.0, instance.getDistance(2, 4), 1e-12);
        assertEquals(0.0, instance.getDistance(3, 3), 1e-12);
    }

    @Test
    void roundsDistancesToNearestIntegerWhenRequested() {
        String text = TestInstances.tinyText().replace("2 0 3", "2 1 1"); // distance to depot becomes sqrt(2)

        VrpInstance exact = VRPLibReader.parse(text, false);
        VrpInstance rounded = VRPLibReader.parse(text, true);

        assertEquals(Math.sqrt(2), exact.getDistance(0, 1), 1e-12);
        assertEquals(1.0, rounded.getDistance(0, 1), 1e-12);
    }

    @Test
    void readsARealCvrplibInstanceFromDisk() throws IOException {
        VrpInstance instance = VRPLibReader.read(TestInstances.P_N16_K8);

        assertEquals("P-n16-k8", instance.getName());
        assertEquals(16, instance.getDimension());
        assertEquals(35, instance.getCapacity());
        assertEquals(0, instance.getDemand(0));
        assertEquals(19, instance.getDemand(1));
        assertEquals(11, instance.getDemand(15));
        // node 1 (30,40) and node 2 (37,52): sqrt(49 + 144)
        assertEquals(Math.sqrt(193), instance.getDistance(0, 1), 1e-12);
    }

    @Test
    void toleratesBlankLinesAndMissingEofAndDepotSection() {
        String text = """
                NAME : minimal
                DIMENSION : 3
                CAPACITY : 100

                NODE_COORD_SECTION
                1 0 0
                2 1 0
                3 0 1
                DEMAND_SECTION
                1 0
                2 1
                3 1
                """;

        VrpInstance instance = VRPLibReader.parse(text, false);

        assertEquals(3, instance.getDimension());
        assertEquals(1.0, instance.getDistance(0, 1), 1e-12);
    }

    @Test
    void acceptsRowsInAnyOrder() {
        String text = TestInstances.tinyText()
                .replace("1 0 0\n2 0 3", "2 0 3\n1 0 0");

        assertEquals(3.0, VRPLibReader.parse(text, false).getDistance(0, 1), 1e-12);
    }

    @Test
    void parsesGoldenDialectWithDepotGivenAsCoordinates() {
        String text = """
                NAME: Golden_like
                TYPE: CVRP
                COMMENT: 1820.09
                DIMENSION: 4
                CAPACITY: 200
                VEHICLES: 2
                DISTANCE: 1000
                EDGE_WEIGHT_TYPE: FUNCTION
                EDGE_WEIGHT_FORMAT: EUC_2D
                NODE_COORD_TYPE: TWOD_COORDS
                NODE_COORD_SECTION
                1 3.00000 0.00000
                2 0.00000 4.00000
                3 -3.00000 0.00000
                DEMAND_SECTION
                1 10
                2 20
                3 30
                DEPOT_SECTION
                0.00000 0.00000
                -1
                EOF
                """;

        VrpInstance instance = VRPLibReader.parse(text, false);

        assertEquals(4, instance.getDimension());
        assertEquals(0, instance.getDemand(0));
        assertEquals(10, instance.getDemand(1));
        assertEquals(30, instance.getDemand(3));
        assertEquals(3.0, instance.getDistance(0, 1), 1e-12);
        assertEquals(4.0, instance.getDistance(0, 2), 1e-12);
        assertEquals(5.0, instance.getDistance(1, 2), 1e-12);
        assertTrue(instance.hasMaxRouteLength());
        assertEquals(1000.0, instance.getMaxRouteLength(), 1e-12);
    }

    @Test
    void standardInstancesHaveNoRouteLengthLimit() {
        VrpInstance instance = TestInstances.tiny();
        assertTrue(!instance.hasMaxRouteLength());
        assertEquals(Double.POSITIVE_INFINITY, instance.getMaxRouteLength());
    }

    @Test
    void readsGoldenInstanceFromDisk() throws IOException {
        VrpInstance instance = VRPLibReader.read(Path.of("datasets", "big", "Golden_20.vrp"));
        assertEquals(421, instance.getDimension());
        assertEquals(200, instance.getCapacity());
        assertEquals(0, instance.getDemand(0));
        // the depot is at the origin and customer 1 at (5.7735, 0)
        assertEquals(5.7735, instance.getDistance(0, 1), 1e-9);
    }

    @Test
    void rejectsGoldenDialectWithWrongCustomerCount() {
        String text = """
                DIMENSION: 5
                CAPACITY: 200
                NODE_COORD_SECTION
                1 3 0
                2 0 4
                3 -3 0
                DEMAND_SECTION
                1 10
                2 20
                3 30
                DEPOT_SECTION
                0 0
                -1
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
        assertTrue(e.getMessage().contains("DIMENSION"), e.getMessage());
    }

    @Test
    void rejectsMissingDimension() {
        String text = TestInstances.tinyText().replace("DIMENSION : 5\n", "");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
        assertTrue(e.getMessage().contains("DIMENSION"), e.getMessage());
    }

    @Test
    void rejectsMissingCapacity() {
        String text = TestInstances.tinyText().replace("CAPACITY : 10\n", "");
        assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
    }

    @Test
    void rejectsUnsupportedEdgeWeightType() {
        String text = TestInstances.tinyText().replace("EUC_2D", "EXPLICIT");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
        assertTrue(e.getMessage().contains("EDGE_WEIGHT_TYPE"), e.getMessage());
    }

    @Test
    void rejectsDepotThatIsNotTheFirstNode() {
        String text = TestInstances.tinyText().replace(" 1\n -1", " 2\n -1");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
        assertTrue(e.getMessage().contains("depot"), e.getMessage());
    }

    @Test
    void rejectsDemandAboveCapacity() {
        String text = TestInstances.tinyText().replace("5 6", "5 11");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
        assertTrue(e.getMessage().contains("exceeds"), e.getMessage());
    }

    @Test
    void rejectsTruncatedSection() {
        String text = TestInstances.tinyText().replace("5 -4 0\n", "");
        assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
    }

    @Test
    void rejectsDuplicateNodeRows() {
        String text = TestInstances.tinyText().replace("5 -4 0", "4 -4 0");
        assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
    }

    @Test
    void rejectsOutOfRangeNodeIds() {
        String text = TestInstances.tinyText().replace("5 -4 0", "6 -4 0");
        assertThrows(IllegalArgumentException.class, () -> VRPLibReader.parse(text, false));
    }

    @Test
    void reportsFileNameOnParseErrors() throws IOException {
        Path file = Files.createTempFile("broken", ".vrp");
        try {
            Files.writeString(file, "DIMENSION : x\n");
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VRPLibReader.read(file));
            assertTrue(e.getMessage().contains(file.toString()), e.getMessage());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
