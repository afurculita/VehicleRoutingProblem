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
    private String instance = "datasets/small/A-n32-k5.vrp";
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
    private Integer TabuHorizon = 10;

    public static void main(String[] args) throws IOException {
        VRPRunner jct = new VRPRunner();
        JCommander jCommander = new JCommander(jct, args);
        jCommander.setProgramName(VRPRunner.class.getSimpleName());

        switch (jct.alg) {
            case "acs":
                Problem problem = new VehicleRoutingProblem(jct.instance);
                VrpAcsSolver aco = new VrpAcsSolver(problem, jct);

                ExecutionStats es = ExecutionStats.execute(aco, problem);
                es.printStats();
                break;
            case "tabu": {
                TabuSearchSolver s = new TabuSearchSolver(
                        new VRPLibReader(new InstanceReader(new File(jct.instance))),
                        jct.TabuHorizon);
                s.solve();
                s.print();
                break;
            }
            default:
            case "greedy": {
                VRPLibReader r = new VRPLibReader(new InstanceReader(new File(jct.instance)));
                GreedySolver s = new GreedySolver(r);
                s.solve();
                s.print();
                break;
            }
        }
    }
}
