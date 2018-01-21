package ro.uaic.info.acs;

import thiagodnf.jacof.aco.AntColonySystem;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.util.ExecutionStats;

import java.io.IOException;

public class VRPRunner {
    public static void main(String[] args) throws IOException {

        Problem problem = new VRPProblem("datasets/att-n48-k4.vrp");

        AntColonySystem aco = new VrpAcs(problem);
        aco.setNumberOfAnts(80);
        aco.setNumberOfIterations(2);
        aco.setAlpha(1.0D);
        aco.setBeta(2.0D);
        aco.setRho(0.1D);
        aco.setOmega(0.1D);
        aco.setQ0(0.9D);

        ExecutionStats es = ExecutionStats.execute(aco, problem);

        es.printStats();
    }
}
