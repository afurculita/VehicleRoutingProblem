package ro.uaic.info;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import ro.uaic.info.acs.VehicleRoutingProblem;
import ro.uaic.info.acs.VrpAcsSolver;
import ro.uaic.info.greedy.GreedySolver;
import ro.uaic.info.tabu.TabuSearchSolver;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.util.ExecutionStats;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;

public class VRPRunner {
    @Parameter(names = {"--algorithm", "-alg"}, required = true)
    private String alg;
    @Parameter(names = {"--instance", "-i"})
    public String instance = "datasets/big/Golden_20.vrp";
    @Parameter(names = "--alpha")
    public double alpha = 1.0D;
    @Parameter(names = "--beta")
    public double beta = 2.0D;
    @Parameter(names = "--rho")
    public double rho = 0.1D;
    @Parameter(names = "--omega")
    public double omega = 0.1D;
    @Parameter(names = "--q0")
    public double q0 = 0.9D;
    @Parameter(names = "--iterations")
    public int iterations = 5;
    @Parameter(names = "--tabu")
    public Integer TabuHorizon = 10;

    public static void main(String[] args) throws IOException {
        VRPRunner jct = new VRPRunner();
        JCommander jCommander = new JCommander(jct, args);
        jCommander.setProgramName(VRPRunner.class.getSimpleName());

        switch (jct.alg) {
            case "acs":
                VrpAcsSolver aco = new VrpAcsSolver(jct);

                ExecutionStats
                        .execute(aco, aco.getProblem())
                        .printStats();
                break;
            case "tabu": {
                new TabuSearchSolver(jct)
                        .solve()
                        .print();
                break;
            }
            default:
            case "greedy": {
                new GreedySolver(jct)
                        .solve()
                        .print();
                break;
            }
        }
    }
}
