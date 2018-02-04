package ro.uaic.info.acs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import ro.uaic.info.VRP;
import thiagodnf.jacof.problem.Problem;

import java.io.IOException;
import java.util.List;

public class VRPRunner {
    @Parameter(names = {"--instance", "-i"})
    public String instance = "X_X-n1001-k43";
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
    public int iterations = 100;

    public static void main(String[] args) throws IOException {
        VRPRunner jct = new VRPRunner();
        JCommander jCommander = new JCommander(jct, args);
        jCommander.setProgramName(VRPRunner.class.getSimpleName());

        System.out.println("Parameters used:");
        System.out.print(jct.toString());

        Problem problem = new VRPProblem("datasets/" + jct.instance + ".vrp");
        VrpAcsSolver aco = new VrpAcsSolver(problem, jct);

        aco.solve();
        aco.print();
    }
}
