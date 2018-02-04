package ro.uaic.info.tabu;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import ro.uaic.info.VRP;
import ro.uaic.info.VRPLibReader;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;

public class VRPRunner {
    @Parameter(names = {"--instance", "-i"})
    private String instance = "datasets/A-n32-k5.vrp";
    @Parameter(names = "--tabu")
    private Integer TabuHorizon = 10;

    public static void main(String[] args) throws IOException {
        VRPRunner jct = new VRPRunner();
        JCommander jCommander = new JCommander(jct, args);
        jCommander.setProgramName(VRPRunner.class.getSimpleName());

        TabuSearchSolver s = new TabuSearchSolver(
                new VRPLibReader(new InstanceReader(new File(jct.instance))),
                jct.TabuHorizon);
        s.solve();
        s.print();
    }
}
