package ro.uaic.info.local;

import org.junit.jupiter.api.Test;
import ro.uaic.info.TestInstances;
import ro.uaic.info.VrpInstance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateListsTest {

    @Test
    void listsNearestNodesFirst() {
        VrpInstance tiny = TestInstances.tiny();
        CandidateLists lists = new CandidateLists(tiny, 2);

        assertEquals(2, lists.size());
        assertFalse(lists.isComplete());
        // customer 1 at (0,3): depot at 3, customers 2 and 4 at 5, customer 3 at 6
        assertEquals(0, lists.of(1)[0]);
        assertTrue(lists.of(1)[1] == 2 || lists.of(1)[1] == 4);
        // customer 3 at (0,-3): depot at 3, then 2 and 4 at 5
        assertEquals(0, lists.of(3)[0]);
    }

    @Test
    void zeroOrLargeKMeansAllOtherNodes() {
        VrpInstance tiny = TestInstances.tiny();
        CandidateLists all = new CandidateLists(tiny, 0);
        CandidateLists large = new CandidateLists(tiny, 100);

        assertTrue(all.isComplete());
        assertTrue(large.isComplete());
        assertEquals(4, all.size());
        assertArrayEquals(all.of(2), large.of(2));
        for (int node = 0; node < tiny.getDimension(); node++) {
            for (int other : all.of(node)) {
                assertTrue(other != node);
            }
        }
    }
}
