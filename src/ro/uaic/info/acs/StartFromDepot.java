package ro.uaic.info.acs;

import thiagodnf.jacof.aco.ACO;
import thiagodnf.jacof.aco.ant.initialization.AbstractAntInitialization;

public class StartFromDepot extends AbstractAntInitialization {
    /**
     * Constructor
     *
     * @param aco The ant colony optimization used
     */
    public StartFromDepot(ACO aco) {
        super(aco);
    }

    @Override
    public int getPosition(int antId) {
        return 0;
    }

    @Override
    public String toString() {
        return StartFromDepot.class.getSimpleName();
    }
}
