package ro.uaic.info.acs;

import org.apache.log4j.Logger;
import thiagodnf.jacof.aco.ACO;
import thiagodnf.jacof.aco.AntColonySystem;
import thiagodnf.jacof.aco.ant.exploration.QSelection;
import thiagodnf.jacof.aco.ant.selection.RouletteWheel;
import thiagodnf.jacof.aco.graph.initialization.ACSInitialization;
import thiagodnf.jacof.aco.rule.globalupdate.deposit.PartialDeposit;
import thiagodnf.jacof.aco.rule.globalupdate.evaporation.FullEvaporation;
import thiagodnf.jacof.aco.rule.localupdate.ACSLocalUpdatingRule;
import thiagodnf.jacof.aco.subset.single.GlobalBest;
import thiagodnf.jacof.problem.Problem;

public class VrpAcs extends AntColonySystem {
    private static final Logger LOGGER = Logger.getLogger(ACO.class);

    VrpAcs(Problem problem) {
        super(problem);
    }

    @Override
    public void build() {
        // Initialization
        setGraphInitialization(new ACSInitialization(this));
        setAntInitialization(new AlwaysFromZeroAntInitialization(this));

        // Exploration
        setAntExploration(new QSelection(this, new RouletteWheel(), q0));

        // Local Update Pheromone Rule
        setAntLocalUpdate(new ACSLocalUpdatingRule(this, omega));

        // Global Update Pheromone Rule
        getEvaporations().add(new FullEvaporation(this, getRho()));
        getDeposits().add(new PartialDeposit(this, getRho(), new GlobalBest(this)));
    }

    @Override
    public int[] solve() {
        LOGGER.info("Starting ACO");

        build();

        printParameters();

        initializePheromones();
        initializeAnts();

        VRPProblem problem = (VRPProblem) getProblem();

        while (!terminationCondition()) {
            problem.reset();
            constructAntsSolutions();
            updatePheromones();
            daemonActions(); // optional
        }

        LOGGER.info("Done");

        return globalBest.getSolution();
    }

    private void constructAntsSolutions() {

        LOGGER.debug("=================== Iteration " + it + " ===================");
        LOGGER.debug("Constructing the ant's solutions");

        //Before construct the ant's solution it is necessary to remove the current best solution
        currentBest = null;

        //Construct the ant solutions by using threads
        for (int k = 0; k < numberOfAnts; k++) {
            ants[k].run();
        }
    }
}
