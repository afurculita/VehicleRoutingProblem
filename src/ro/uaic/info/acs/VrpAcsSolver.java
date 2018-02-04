package ro.uaic.info.acs;

import org.apache.log4j.Logger;
import thiagodnf.jacof.aco.ACO;
import thiagodnf.jacof.aco.AntColonySystem;
import thiagodnf.jacof.aco.ant.Ant;
import thiagodnf.jacof.aco.ant.exploration.QSelection;
import thiagodnf.jacof.aco.ant.selection.RouletteWheel;
import thiagodnf.jacof.aco.graph.initialization.ACSInitialization;
import thiagodnf.jacof.aco.rule.globalupdate.deposit.PartialDeposit;
import thiagodnf.jacof.aco.rule.globalupdate.evaporation.FullEvaporation;
import thiagodnf.jacof.aco.rule.localupdate.ACSLocalUpdatingRule;
import thiagodnf.jacof.aco.subset.single.GlobalBest;
import thiagodnf.jacof.problem.Problem;

public class VrpAcsSolver extends AntColonySystem {
    private static final Logger LOGGER = Logger.getLogger(ACO.class);

    VrpAcsSolver(Problem problem, VRPRunner jct) {
        super(problem);

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

    @Override
    public void build() {
        // Initialization
        setGraphInitialization(new ACSInitialization(this));
        setAntInitialization(new StartFromDepot(this));

        // Exploration
        setAntExploration(new QSelection(this, new RouletteWheel(), q0));

        // Local Update Pheromone Rule
        setAntLocalUpdate(new ACSLocalUpdatingRule(this, omega));

        // Global Update Pheromone Rule
        getEvaporations().add(new FullEvaporation(this, getRho()));
        getDeposits().add(new PartialDeposit(this, getRho(), new GlobalBest(this)));
    }
}
