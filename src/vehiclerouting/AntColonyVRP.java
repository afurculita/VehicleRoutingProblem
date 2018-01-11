package vehiclerouting;

import com.ugos.acs.Ant;
import com.ugos.acs.AntColony;
import com.ugos.acs.AntGraph;

import java.util.Random;

public class AntColonyVRP extends AntColony {
    protected static final double A = 0.1;

    private long m_capacity = 0;
    private long[] m_demands = null;

    public AntColonyVRP(AntGraph graph, int ants, int iterations, long _capacity, long[] _demands) {
        super(graph, ants, iterations);

        m_capacity = _capacity;
        m_demands = _demands;
    }

    protected Ant[] createAnts(AntGraph graph, int nAnts) {
        Random ran = new Random(System.currentTimeMillis());
        AntVRP.reset();
        AntVRP.setAntColony(this);
        AntVRP ant[] = new AntVRP[nAnts];
        for (int i = 0; i < nAnts; i++) {
            ant[i] = new AntVRP(graph.nodes() - 1, this);
        }

        return ant;
    }

    protected void globalUpdatingRule() {
        double dEvaporation = 0;
        double dDeposition = 0;

        for (int r = 0; r < m_graph.nodes() - 1; r++) {
            for (int s = 0; s < m_graph.nodes() - 1; s++) {
                if (r != s) {
                    // get the value for deltatau
                    double deltaTau = // Ant4TSP.s_dBestPathValue *
                            // (double)Ant4TSP.s_bestPath[r][s];
                            ((double) 1 / AntVRP.s_dBestPathValue) * (double) AntVRP.s_bestPath[r][s];

                    // get the value for phermone evaporation as defined in eq.
                    // d)
                    dEvaporation = ((double) 1 - A) * m_graph.tau(r, s);
                    // get the value for phermone deposition as defined in eq.
                    // d)
                    dDeposition = A * deltaTau;

                    // update tau
                    m_graph.updateTau(r, s, dEvaporation + dDeposition);
                }
            }
        }
    }

    @Override
    public String getFolder() {
        return "results/temp_";
    }

    public long getCapacity() {
        return m_capacity;
    }

    public long[] getDemands() {
        return m_demands;
    }
}