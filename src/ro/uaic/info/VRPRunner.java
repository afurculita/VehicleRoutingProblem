package ro.uaic.info;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import thiagodnf.jacof.aco.AntColonySystem;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.problem.kp.KnapsackProblem;
import thiagodnf.jacof.util.ExecutionStats;

import java.io.IOException;

public class VRPRunner {
    static final Logger LOGGER = Logger.getLogger(VRPRunner.class);

    public VRPRunner() {
    }

    public static void main(String[] args) throws ParseException, IOException {
        String instance = "datasets/p06.kp";
        Problem problem = new KnapsackProblem(instance);
        AntColonySystem aco = new AntColonySystem(problem);
        aco.setNumberOfAnts(50);
        aco.setNumberOfIterations(3000);
        aco.setAlpha(1.0D);
        aco.setBeta(2.0D);
        aco.setRho(0.1D);
        aco.setOmega(0.1D);
        aco.setQ0(0.9D);
        ExecutionStats es = ExecutionStats.execute(aco, problem);
        es.printStats();
    }
}
