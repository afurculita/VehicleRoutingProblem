package ro.uaic.info;

import org.junit.jupiter.api.Test;
import ro.uaic.info.acs.AcsParameters;
import ro.uaic.info.acs.AntColonySystemSolver;
import ro.uaic.info.greedy.ClarkeWrightSolver;
import ro.uaic.info.greedy.GreedySolver;
import ro.uaic.info.local.CandidateLists;
import ro.uaic.info.tabu.TabuSearchSolver;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The maximum route length of the Golden / Li instances is enforced everywhere. */
class RouteLengthConstraintTest {

    private final VrpInstance limited = TestInstances.limited();

    @Test
    void solutionValidationRejectsOverlongRoutes() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Solution.of(limited, List.of(List.of(1, 2, 3), List.of(4, 5), List.of(6, 7), List.of(8))));
        assertTrue(e.getMessage().contains("maximum route length"), e.getMessage());
        assertEquals(4, Solution.of(limited, List.of(List.of(1, 2), List.of(3, 4), List.of(5, 6), List.of(7, 8))).getNumberOfRoutes());
    }

    @Test
    void everySolverRespectsTheLimit() {
        Random random = new Random(1);
        List<Solver> solvers = List.of(
                new GreedySolver(limited),
                new ClarkeWrightSolver(limited),
                new TabuSearchSolver(limited, 3, 200, 0.1, random, new CandidateLists(limited, 0)),
                new AntColonySystemSolver(limited, AcsParameters.DEFAULT.withIterations(5), random));
        for (Solver solver : solvers) {
            Solution solution = assertDoesNotThrow(solver::solve, solver.getClass().getSimpleName()); // Solution.of validates
            assertEquals(4, solution.getNumberOfRoutes(), solver.getClass().getSimpleName());
            for (List<Integer> route : solution.getRoutes()) {
                assertTrue(Solution.routeCost(limited, route) <= 30.0 + 1e-9);
            }
        }
    }

    @Test
    void optimumPairsNeighboursOnTheCircle() {
        Solution best = new TabuSearchSolver(limited, 3, 200, 0.0, new Random(2), new CandidateLists(limited, 0)).solve();
        // four routes of two adjacent customers: 4 * (10 + 7.654 + 10)
        assertEquals(4 * (20 + 7.6537), best.getCost(), 1e-2);
    }
}
