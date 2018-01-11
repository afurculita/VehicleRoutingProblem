package vehiclerouting;

import com.ugos.acs.Ant;
import com.ugos.acs.AntGraph;

import java.util.*;

public class AntVRP extends Ant {
    private static final double B = 2;
    private static final double Q0 = 0.8;
    private static final double R = 0.1;

    private static final Random s_randGen = new Random(System.currentTimeMillis());

    private Hashtable<Integer, Integer> m_nodesToVisitTbl;
    private Hashtable<Integer, Integer> m_nodesToVisitTblLater;
    private long m_sumDemand = 0;
    private long[] m_demands;
    private long m_capacity;

    public AntVRP(int startNode, Observer observer) {
        super(startNode, observer);
    }

    public void init() {
        super.init();

        final AntGraph graph = s_antColony.getGraph();

        // inizializza l'array di citt‡ da visitare
        m_nodesToVisitTbl = new Hashtable<>(graph.nodes());
        m_nodesToVisitTblLater = new Hashtable<>();
        for (int i = 0; i < graph.nodes(); i++)
            m_nodesToVisitTbl.put(i, i);

        // Remove the current city
        // m_nodesToVisitTbl.remove(new Integer(m_nStartNode));

        // nExplore = 0;

        m_demands = ((AntColonyVRP) s_antColony).getDemands();
        m_sumDemand = 0;
        m_capacity = ((AntColonyVRP) s_antColony).getCapacity();
    }

    public int stateTransitionRule(int nCurNode) {
        final AntGraphVRP graph = (AntGraphVRP) s_antColony.getGraph();

        // generate a random number
        double q = s_randGen.nextDouble();
        int nMaxNode = -1;

        int s = m_nodesToVisitTbl.size();
        // m_nodesToVisitTbl = removeTooBigItems(m_nodesToVisitTbl);
        // System.out.println("stateTransitionRule nodesLeft start " + s);

        if (q <= Q0) // Exploitation
        {
            // System.out.print("Exploitation: ");
            double dMaxVal = -1;
            double dVal;
            int nNode;

            // search the max of the value as defined in Eq. a)
            Enumeration enu = m_nodesToVisitTbl.elements();

            while (enu.hasMoreElements()) {
                // select a node
                nNode = (Integer) enu.nextElement();

                // check on tau
                if (graph.tau(nCurNode, nNode) == 0)
                    throw new RuntimeException("tau = 0");

                // get the value
                dVal = graph.tau(nCurNode, nNode) * Math.pow(graph.etha(nCurNode, nNode), B);

                // check if it is the max
                if (dVal > dMaxVal) {
                    dMaxVal = dVal;
                    nMaxNode = nNode;
                }
            }
        } else // Exploration
        {
            // System.out.println("Exploration");
            double dSum = 0;
            int nNode = -1;

            // get the sum at denominator
            Enumeration enu = m_nodesToVisitTbl.elements();

            while (enu.hasMoreElements()) {
                nNode = (Integer) enu.nextElement();
                if (graph.tau(nCurNode, nNode) == 0)
                    throw new RuntimeException("tau = 0");

                // Update the sum
                dSum += graph.tau(nCurNode, nNode) * Math.pow(graph.etha(nCurNode, nNode), B);
            }

            if (dSum == 0)
                throw new RuntimeException("SUM = 0");

            // get the everage value
            double dAverage = dSum / (double) m_nodesToVisitTbl.size();

            // search the node in agreement with eq. b)
            enu = m_nodesToVisitTbl.elements();
            while (enu.hasMoreElements() && nMaxNode < 0) {
                nNode = (Integer) enu.nextElement();

                // get the value of p as difined in eq. b)
                double p = (graph.tau(nCurNode, nNode) * Math.pow(graph.etha(nCurNode, nNode), B)) / dSum;

                // if the value of p is greater the the average value the node
                // is good
                if ((graph.tau(nCurNode, nNode) * Math.pow(graph.etha(nCurNode, nNode), B)) > dAverage) {
                    // System.out.println("Found");
                    nMaxNode = nNode;
                }
            }

            if (nMaxNode == -1)
                nMaxNode = nNode;
        }

        if (nMaxNode == -1) {
            Enumeration enu = m_nodesToVisitTbl.elements();
            nMaxNode = (Integer) enu.nextElement();
        }
        if (nMaxNode < 0)
            throw new RuntimeException("maxNode = -1 " + s);

        // delete the selected node from the list of node to visit
        if (m_demands[nMaxNode] > 0)
            m_nodesToVisitTbl.remove(nMaxNode);
        // System.out.println("stateTransitionRule nodesLeft end " + s + " " +
        // nMaxNode);
        m_sumDemand += m_demands[nMaxNode];
        if (m_sumDemand < 0)
            m_sumDemand = 0;
//		System.out.println("stateTransitionRule m_sumDemand end " + m_sumDemand);
        return nMaxNode;
    }

    public void localUpdatingRule(int nCurNode, int nNextNode) {
        final AntGraph graph = s_antColony.getGraph();
        // update tau only for distances between customers
        if (nCurNode != graph.nodes() - 1 && nNextNode != graph.nodes() - 1) {
            // get the value of the Eq. c)
            double val = ((double) 1 - R) * graph.tau(nCurNode, nNextNode) + (R * (graph.tau0()));

            // update tau
            graph.updateTau(nCurNode, nNextNode, val);
        }
    }

    public boolean better(double dPathValue1, double dPathValue2) {
        return dPathValue1 < dPathValue2;
    }

    public boolean end() {
        reorderItems();

        return m_nodesToVisitTblLater.isEmpty() && m_nodesToVisitTbl.size() == 1;
    }

    public void reorderItems() {
        double spaceLeft = m_capacity - m_sumDemand;

        // copy all back
        Enumeration enu = m_nodesToVisitTblLater.elements();
        while (enu.hasMoreElements()) {
            int nNode = (Integer) enu.nextElement();
            m_nodesToVisitTbl.put(nNode, nNode);
        }
        m_nodesToVisitTblLater.clear();

        ArrayList<Integer> toRemove = new ArrayList<>();
        enu = m_nodesToVisitTbl.elements();
        while (enu.hasMoreElements()) {
            int nNode = (Integer) enu.nextElement();
            if (m_demands[nNode] > spaceLeft)
                toRemove.add(nNode);
        }
        // save the orders for later
        for (Integer i : toRemove) {
            m_nodesToVisitTbl.remove(i);
            m_nodesToVisitTblLater.put(i, i);
        }

        if (m_nodesToVisitTbl.size() > 1 && m_nodesToVisitTbl.contains(m_nStartNode)) {
            m_nodesToVisitTbl.remove(m_nStartNode);
            m_nodesToVisitTblLater.put(m_nStartNode, m_nStartNode);
        }

    }

    @Override
    public String getFolder() {
        return "results/temp_";
    }
}