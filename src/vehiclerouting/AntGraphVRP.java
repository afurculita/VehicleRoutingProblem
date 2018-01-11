package vehiclerouting;

import com.ugos.acs.AntGraph;

import java.util.Vector;

public class AntGraphVRP extends AntGraph {

    public AntGraphVRP(int _nodes, double[][] _delta) {
        super(_nodes, _delta);
    }

    // get the value
    public synchronized double delta(int r, int s, Vector path) {
        double val = 0;
        double sum = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            val = m_delta[(int) path.get(i)][(int) path.get(i + 1)];
            sum += val;
        }

        val = m_delta[s][s];
        sum += val;

        return sum;
    }
}