package ro.uaic.info;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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
        return VRPLibReader.parse(tinyText(), false);
    }

    public static VrpInstance pn16k8() {
        try {
            return VRPLibReader.read(P_N16_K8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
