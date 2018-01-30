package ro.uaic.info.tabu;

import ro.uaic.info.VRP;
import ro.uaic.info.VRPLibReader;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;

public class VRPRunner {
    public static void main(String[] args) throws IOException {
        TabuSearchSolver s = new TabuSearchSolver(
                new VRPLibReader(new InstanceReader(new File("datasets/" + VRP.FILE + ".vrp"))),
                20,
                10);
        s.solve();
        s.print();
    }
}
