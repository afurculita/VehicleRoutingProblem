package ro.uaic.info;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import ro.uaic.info.VRPLibReader.DistanceMode;

/** Shared fixtures for the test suite. */
public final class TestInstances {

    /** Optimal cost of the {@code tiny.vrp} fixture: routes {1,2}, {3}, {4}. */
    public static final double TINY_OPTIMUM = 26.0;

    /** A real CVRPLIB instance shipped with the repository (best known value 435 with rounded distances). */
    public static final Path P_N16_K8 = Path.of("datasets", "small", "P-n16-k8.vrp");

    private TestInstances() {
    }

    public static String tinyText() {
        try (InputStream in = TestInstances.class.getResourceAsStream("/tiny.vrp")) {
            if (in == null) {
                throw new IllegalStateException("tiny.vrp fixture not found on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static VrpInstance tiny() {
        return VRPLibReader.parse(tinyText(), DistanceMode.AUTO);
    }

    /** P-n16-k8 with rounded distances (the default for this dialect). */
    public static VrpInstance pn16k8() {
        return pn16k8(DistanceMode.AUTO);
    }

    public static VrpInstance pn16k8(DistanceMode mode) {
        try {
            return VRPLibReader.read(P_N16_K8, mode);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Eight customers on a circle of radius 10 with a route-length limit; see {@link #LIMITED}. */
    public static VrpInstance limited() {
        return VRPLibReader.parse(LIMITED, DistanceMode.EXACT);
    }

    /** Eight customers on a circle of radius 10; a route may hold at most two of them (10 + 7.65 + 10 < 30 < 35.3). */
    public static final String LIMITED = """
            NAME: limited
            DIMENSION: 9
            CAPACITY: 100
            DISTANCE: 30
            EDGE_WEIGHT_TYPE: FUNCTION
            EDGE_WEIGHT_FORMAT: EUC_2D
            NODE_COORD_SECTION
            1 10 0
            2 7.0711 7.0711
            3 0 10
            4 -7.0711 7.0711
            5 -10 0
            6 -7.0711 -7.0711
            7 0 -10
            8 7.0711 -7.0711
            DEMAND_SECTION
            1 1
            2 1
            3 1
            4 1
            5 1
            6 1
            7 1
            8 1
            DEPOT_SECTION
            0 0
            -1
            EOF
            """;
}
