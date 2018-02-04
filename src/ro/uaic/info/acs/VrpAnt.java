package ro.uaic.info.acs;

import thiagodnf.jacof.aco.ACO;
import thiagodnf.jacof.aco.ant.Ant;

public class VrpAnt extends Ant {
    public int currentCapacity;

    /**
     * Constructor
     *
     * @param aco The ant colony optimization
     * @param id  The ant's id
     */
    public VrpAnt(ACO aco, int id) {
        super(aco, id);
    }

    /**
     * Construct the ant's solution
     */
    public void explore() {
        VehicleRoutingProblem problem = (VehicleRoutingProblem) aco.getProblem();

        // The search ends when the list of nodes to visit is empty
        while (!nodesToVisit.isEmpty()) {
            // Get the next node given the current node
            int nextNode = aco.getAntExploration().getNextNode(this, currentNode);

            // Perform the local update rule if this is available
            if (aco.getAntLocalUpdate() != null) {
                aco.getAntLocalUpdate().update(currentNode, nextNode);
            }

            // Save the next node in the tour
            tour.add(new Integer(nextNode));
            if (nextNode == 0) // is depot
                currentCapacity = 0;
            else
                currentCapacity += problem.getDemands()[nextNode];

            // Mark as visited the arc(i,j)
            path[currentNode][nextNode] = 1;
            path[nextNode][currentNode] = 1;

            // update the list of the nodes to visit
            nodesToVisit = problem.recomputeNodesToVisit(tour, this);

            // Define the next node as current node
            currentNode = nextNode;
        }
    }
}
