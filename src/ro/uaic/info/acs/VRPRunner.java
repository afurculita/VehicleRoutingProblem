package ro.uaic.info.acs;

import thiagodnf.jacof.aco.AntColonySystem;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.util.ExecutionStats;

import java.io.IOException;

public class VRPRunner {
    public static void main(String[] args) throws IOException {

        Problem problem = new VRPProblem("datasets/1a2b3a4a.vrp");

        AntColonySystem aco = new AntColonySystem(problem);
        aco.setNumberOfAnts(50);
        aco.setNumberOfIterations(3);
        aco.setAlpha(1.0D);
        aco.setBeta(2.0D);
        aco.setRho(0.1D);
        aco.setOmega(0.1D);
        aco.setQ0(0.9D);

        ExecutionStats es = ExecutionStats.execute(aco, problem);

        es.printStats();
    }
}
