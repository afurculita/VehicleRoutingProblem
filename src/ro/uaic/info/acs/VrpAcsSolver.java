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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class VrpAcsSolver extends AntColonySystem {
    private static final Logger LOGGER = Logger.getLogger(ACO.class);

    private Map<Integer, Double> iterationResults = new HashMap<>();
    private List<Ant> iterationAnts = new ArrayList<>();

    private double bestSolutionOverall = 0.0;
    private List<Ant> bestSolutionAnts = new ArrayList<>();

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

    @Override
    public void build() {
        // Initialization
        setGraphInitialization(new ACSInitialization(this));
        setAntInitialization(new StartFromDeposit(this));

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

    /**
     * When an ant has finished its search process, this method is called to
     * update the current and global best solutions.
     */
    @Override
    public synchronized void update(Observable obj, Object arg) {
        Ant ant = (Ant) obj;

        // Calculate the fitness function for the found solution
        ant.setTourLength(problem.evaluate(ant.getSolution()));
        iterationAnts.add(ant);

        // Update the current best solution
        if (currentBest == null || problem.better(ant.getTourLength(), currentBest.getTourLength())) {
            currentBest = ant.clone();
        }

        // Update the global best solution
        if (globalBest == null || problem.better(ant.getTourLength(), globalBest.getTourLength())) {
            globalBest = ant.clone();
        }

        LOGGER.debug(ant);

        iterationResults.put(ant.getId(), ant.getTourLength());

        // Verify if all ants have finished their search
        if (++finishedAnts == numberOfAnts) {
            // Restart the counter to build the solutions again
            finishedAnts = 0;

            AtomicReference<Double> solution = new AtomicReference<>((double) 0);

            iterationResults.forEach((Integer k, Double v) -> solution.set(v + solution.get()));

            LOGGER.debug("Current: " + solution);

            if ((bestSolutionOverall == 0.0) || solution.get() < bestSolutionOverall) {
                bestSolutionOverall = solution.get();
                bestSolutionAnts.clear();
                bestSolutionAnts.addAll(iterationAnts);
            }

            iterationResults.clear();
            iterationAnts.clear();
            // Continue all execution
            notify();
        }
    }

    public void print() {
        LOGGER.info("==================================================");
        LOGGER.info("Best Solution: " + bestSolutionOverall);
        LOGGER.info("Paths: ");
        bestSolutionAnts.forEach((Ant ant) -> {
            if (ant.getTourLength() > 0)
                LOGGER.info(ant.toString());
        });
        LOGGER.info("==================================================");
    }
}
