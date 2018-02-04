package ro.uaic.info.acs;

import org.apache.log4j.Logger;
import ro.uaic.info.VRPRunner;
import thiagodnf.jacof.aco.ACO;
import thiagodnf.jacof.aco.AntColonySystem;
import thiagodnf.jacof.aco.ant.Ant;
import thiagodnf.jacof.problem.Problem;

import java.io.IOException;

public class VrpAcsSolver extends AntColonySystem {
    private static final Logger LOGGER = Logger.getLogger(ACO.class);

    public VrpAcsSolver(VRPRunner jct) throws IOException {
        super(new VehicleRoutingProblem(jct.instance));

        this.setNumberOfAnts(problem.getNumberOfNodes());
        this.setNumberOfIterations(jct.iterations);
        this.setAlpha(jct.alpha);
        this.setBeta(jct.beta);
        this.setRho(jct.rho);
        this.setOmega(jct.omega);
        this.setQ0(jct.q0);
    }

    /**
     * Initialize the ants. This method creates an array of ants
     * and positions them in one of the graph's vertex
     */
    protected void initializeAnts() {
        LOGGER.debug("Initializing the ants");

        this.ants = new Ant[numberOfAnts];

        for (int k = 0; k < numberOfAnts; k++) {
            ants[k] = new VrpAnt(this, k);
            ants[k].setAntInitialization(getAntInitialization());
            ants[k].addObserver(this);
        }
    }
}
