package ro.uaic.info.tabu;

import ro.uaic.info.VRPLibReader;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;

public class VRPRunner {
    public static void main(String[] args) throws IOException {
        TabuSearchSolver s = new TabuSearchSolver(
                new VRPLibReader(new InstanceReader(new File("datasets/att-n48-k4.vrp"))),
                4,
                10);
        s.solve();
        s.print();
    }
}
