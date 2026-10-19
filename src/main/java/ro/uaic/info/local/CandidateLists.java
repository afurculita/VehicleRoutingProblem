package ro.uaic.info.local;

import ro.uaic.info.VrpInstance;

import java.util.Arrays;
import java.util.Comparator;

/**
 * For every node, the list of its {@code k} nearest other nodes (depot included), sorted by distance.
 *
 * <p>Restricting a neighbourhood to moves that create at least one arc between a node and one of
 * its candidates (a <em>granular</em> neighbourhood, Toth &amp; Vigo 2003) reduces the cost of a scan
 * from O(n²) to O(n·k) with a negligible loss of quality for {@code k} around 20–40.
 */
public final class CandidateLists {

    private final int[][] neighbours;

    /**
     * @param k number of neighbours per node; {@code 0} or anything {@code >= n - 1} means "all nodes"
     */
    public CandidateLists(VrpInstance instance, int k) {
        int n = instance.getDimension();
        int size = k <= 0 ? n - 1 : Math.min(k, n - 1);
        neighbours = new int[n][];
        Integer[] order = new Integer[n - 1];
        for (int i = 0; i < n; i++) {
            int fill = 0;
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    order[fill++] = j;
                }
            }
            final int from = i;
            Arrays.sort(order, Comparator.comparingDouble(j -> instance.getDistance(from, j)));
            neighbours[i] = new int[size];
            for (int p = 0; p < size; p++) {
                neighbours[i][p] = order[p];
            }
        }
    }

    /** The candidates of {@code node}, nearest first. The returned array must not be modified. */
    public int[] of(int node) {
        return neighbours[node];
    }

    public int size() {
        return neighbours.length == 0 ? 0 : neighbours[0].length;
    }

    /** Whether every node lists every other node, i.e. the neighbourhood is not restricted. */
    public boolean isComplete() {
        return size() == neighbours.length - 1;
    }
}
