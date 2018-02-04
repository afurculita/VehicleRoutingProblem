package ro.uaic.info.acs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import thiagodnf.jacof.problem.Problem;
import thiagodnf.jacof.util.ExecutionStats;

import java.io.IOException;

public class VRPRunner {
    @Parameter(names = {"--instance", "-i"})
    public String instance = "A-n32-k5.vrp";
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

    public static void main(String[] args) throws IOException {
        VRPRunner jct = new VRPRunner();
        JCommander jCommander = new JCommander(jct, args);
        jCommander.setProgramName(VRPRunner.class.getSimpleName());

        Problem problem = new VehicleRoutingProblem("datasets/" + jct.instance);
        VrpAcsSolver aco = new VrpAcsSolver(problem, jct);

        ExecutionStats es = ExecutionStats.execute(aco, problem);
        es.printStats();
    }
}
