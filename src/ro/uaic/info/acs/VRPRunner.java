package ro.uaic.info.acs;

import ro.uaic.info.VRP;
import thiagodnf.jacof.problem.Problem;

import java.io.IOException;

public class VRPRunner {
    public static void main(String[] args) throws IOException {
        Problem problem = new VRPProblem("datasets/" + VRP.FILE + ".vrp");

        VrpAcsSolver aco = new VrpAcsSolver(problem);
        aco.setNumberOfAnts(80);
        aco.setNumberOfIterations(100);
        aco.setAlpha(1.0D);
        aco.setBeta(2.0D);
        aco.setRho(0.1D);
        aco.setOmega(0.1D);
        aco.setQ0(0.9D);

        aco.solve();
        aco.print();
    }
}
