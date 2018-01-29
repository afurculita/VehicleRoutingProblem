package ro.uaic.info.greedy;

import ro.uaic.info.VRPLibReader;
import thiagodnf.jacof.util.io.InstanceReader;

import java.io.File;
import java.io.IOException;

public class VRPRunner {
    public static void main(String[] args) throws IOException {
        VRPLibReader r = new VRPLibReader(new InstanceReader(new File("datasets/att-n48-k4.vrp")));
        GreedySolver s = new GreedySolver(r, 6);
        s.solve();
        s.print();
    }
}
