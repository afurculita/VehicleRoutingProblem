package ro.uaic.info.acs;

import org.junit.jupiter.api.Test;
import ro.uaic.info.acs.AcsParameters.LocalSearchMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcsParametersTest {

    @Test
    void defaultsAreTheClassicAcsSettings() {
        AcsParameters p = AcsParameters.DEFAULT;
        assertEquals(1.0, p.alpha());
        assertEquals(2.0, p.beta());
        assertEquals(0.1, p.rho());
        assertEquals(0.1, p.omega());
        assertEquals(0.9, p.q0());
        assertEquals(10, p.numberOfAnts());
        assertEquals(100, p.iterations());
        assertEquals(LocalSearchMode.FULL, p.localSearch());
        assertEquals(7, p.withIterations(7).iterations());
    }

    @Test
    void rejectsNullLocalSearchMode() {
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 0.1, 0.1, 0.9, 10, 0, null));
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(-1, 2, 0.1, 0.1, 0.9, 10, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, -2, 0.1, 0.1, 0.9, 10, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 0.0, 0.1, 0.9, 10, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 1.0, 0.1, 0.9, 10, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 0.1, 1.5, 0.9, 10, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 0.1, 0.1, 1.1, 10, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 0.1, 0.1, 0.9, 0, 0, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(1, 2, 0.1, 0.1, 0.9, 10, -1, LocalSearchMode.FULL));
        assertThrows(IllegalArgumentException.class, () -> new AcsParameters(Double.NaN, 2, 0.1, 0.1, 0.9, 10, 0, LocalSearchMode.FULL));
    }
}
