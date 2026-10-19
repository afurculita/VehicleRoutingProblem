# Vehicle Routing Problem

[![CI](https://github.com/afurculita/VehicleRoutingProblem/actions/workflows/ci.yml/badge.svg)](https://github.com/afurculita/VehicleRoutingProblem/actions/workflows/ci.yml)

Metaheuristics for the *Capacitated Vehicle Routing Problem (CVRP)* in plain Java: a greedy
construction heuristic, a **Tabu Search** and an **Ant Colony System**, plus a reader for the
TSPLIB / [CVRPLIB](http://vrp.atd-lab.inf.puc-rio.br/index.php/en/) instance format and
[irace](https://iridia.ulb.ac.be/irace/) scenarios for parameter tuning.

The Vehicle Routing Problem is a well known combinatorial optimization problem and a
generalization of the travelling salesman problem: a fleet of vehicles based at a single depot has
to deliver goods to a set of customers at minimal cost. Finding the optimal solution is NP-hard, so
heuristics are used to approximate it. See https://en.wikipedia.org/wiki/Vehicle_routing_problem.

The variant solved here is the CVRP. Let _G = (V, A)_ be a graph where _V_ is the vertex set and _A_
the arc set. One vertex is the depot, at which a fleet of identical vehicles of capacity _Q_ is based;
the other vertices are customers with a demand _q<sub>i</sub>_. Each arc _(v<sub>i</sub>, v<sub>j</sub>)_
has a cost _c<sub>ij</sub>_ (here: the Euclidean distance). The CVRP consists in finding a set of routes
such that:

1. each route begins and ends at the depot;
2. each customer is visited exactly once by exactly one route;
3. the total demand of the customers assigned to each route does not exceed _Q_;
4. the total cost of the routes is minimized.

## Quick start

Requirements: JDK 21+ and Maven 3.6.3+. There are no runtime dependencies besides
[JCommander](https://jcommander.org/) (command line parsing), and the build only needs Maven Central.

```bash
mvn package                              # compiles, runs the tests, builds target/vrp-2.0.0.jar
java -jar target/vrp-2.0.0.jar -alg tabu -i datasets/small/A-n32-k5.vrp --seed 1
java -jar target/vrp-2.0.0.jar -alg acs  -i datasets/small/A-n32-k5.vrp --seed 1
java -jar target/vrp-2.0.0.jar --help
```

Example output:

```
Instance: A-n32-k5 (31 customers, capacity 100, at least 5 vehicles)
Algorithm: tabu
Seed: 1
Execution Time: 220 ms
=========================================================
Vehicle 0: 0 -> 30 -> 16 -> 1 -> 12 -> 0
Vehicle 1: 0 -> 27 -> 24 -> 0
...

Best Value: 787.0819
```

The `Best Value:` line is what the irace scripts parse; its format is stable.

### Command line options

| Option | Applies to | Default | Meaning |
| --- | --- | --- | --- |
| `-alg`, `--algorithm` | all | required | `greedy`, `tabu` or `acs` |
| `-i`, `--instance` | all | required | path to a `.vrp` file |
| `--seed` | all | random | random seed; the seed used is always printed, so any run can be reproduced |
| `--round-distances` | all | off | round distances to the nearest integer (TSPLIB `EUC_2D` convention, see below) |
| `--iterations` | tabu, acs | 10000 / 100 | tabu: consecutive non-improving iterations before stopping; acs: colony iterations |
| `--tabu` | tabu | 5 | base tabu tenure in iterations (a random 0–4 is added) |
| `--alpha` | acs | 1.0 | exponent of the pheromone trail τ |
| `--beta` | acs | 2.0 | exponent of the visibility η = 1/d |
| `--rho` | acs | 0.1 | global pheromone evaporation / deposit rate, in (0, 1) |
| `--omega` | acs | 0.1 | local pheromone update rate, in [0, 1] |
| `--q0` | acs | 0.9 | probability of the greedy (exploitation) step, in [0, 1] |
| `--ants` | acs | one per customer | ants per iteration |
| `--local-search` | acs | true | improve every ant's routes with 2-opt |

Exit codes: `0` success, `1` the instance could not be read or a parameter value is invalid,
`2` the command line could not be parsed.

### Using the solvers from Java

```java
VrpInstance instance = VRPLibReader.read(Path.of("datasets/small/A-n32-k5.vrp"));
Solution solution = new TabuSearchSolver(instance, 5, 10_000, new Random(1)).solve();
solution.getCost();      // 787.08...
solution.getRoutes();    // [[30, 16, 1, 12], [27, 24], ...]  (customers only; the depot is node 0)
```

`Solution.of(instance, routes)` validates that every customer is served exactly once and no route
exceeds the capacity, so a solver can never return an infeasible solution unnoticed.

## Project layout

```
src/main/java/ro/uaic/info/
  VRPRunner.java                 command line entry point
  VRPLibReader.java              TSPLIB / CVRPLIB parser
  VrpInstance.java               immutable problem data (demands, distance matrix, capacity)
  Solution.java                  routes + cost, feasibility checks, output formatting
  Solver.java                    the interface every algorithm implements
  greedy/GreedySolver.java       nearest-neighbour construction heuristic
  tabu/TabuSearchSolver.java     tabu search (relocate, swap, 2-opt; perturbation on stagnation)
  acs/AntColonySystemSolver.java ant colony system
  acs/AcsParameters.java         ACS parameter record with validation
  local/TwoOpt.java              intra-route 2-opt descent shared by the solvers
src/test/java/                   JUnit 6 test suite (run with `mvn test`)
datasets/small/                  Augerat A, B and P instances (16–101 nodes)
datasets/big/                    Christofides M, Uchoa X, Golden and Li instances (101–1200 nodes)
tuning/{tabu,acs}/               irace scenarios
```

## Instances

Instances come from [CVRPLIB](http://vrp.atd-lab.inf.puc-rio.br/index.php/en/) in the
[TSPLIB95](https://www.iwr.uni-heidelberg.de/groups/comopt/software/TSPLIB95/tsp95.pdf) format.
Two dialects are supported:

* the standard one (A, B, P, M, X sets): all nodes, depot included, are listed in
  `NODE_COORD_SECTION` and the depot is node 1;
* the Golden / Li one: `EDGE_WEIGHT_TYPE: FUNCTION` with `EDGE_WEIGHT_FORMAT: EUC_2D`, only the
  customers are listed and the depot is given as an `x y` pair in `DEPOT_SECTION`.

Internally the depot is always node `0` and customers are `1 .. n-1`.

**Distance rounding.** TSPLIB defines `EUC_2D` distances as Euclidean distances *rounded to the
nearest integer*, and the best known values of the A, B, P, M and X sets use that convention. The
Golden and Li sets are evaluated with real-valued distances. By default this program uses
real-valued distances (which is what the original implementation did); pass `--round-distances` to
compare against the integer-based optima of CVRPLIB.

**Route length limits.** Twenty of the Golden and Li instances declare a maximum route length
(`DISTANCE`). It is read and reported with a warning but *not enforced* by any solver, so results on
those instances are not comparable to the literature.

## Algorithms

### Greedy (nearest neighbour)

Starting at the depot, the vehicle repeatedly drives to the closest unserved customer whose demand
still fits. When nothing fits, the route is closed and a new vehicle starts. The result is used as the
starting point of the Tabu Search and to initialise the pheromone trails of the ACS.

### Tabu Search

Tabu search is a metaheuristic that drives a local search beyond local optima: at every step the
best move of the neighbourhood is applied *even if it worsens the solution*, and recently reversed
attributes are declared *tabu* (forbidden) for a number of iterations so that the search does not
immediately come back.

<img src="./resources/Flow-chart-of-tabu-search-algorithm.png" width="400" />

Implementation (`tabu/TabuSearchSolver.java`):

* **Neighbourhood** (scanned completely at every iteration, O(n²)):
  *relocate* (move one customer anywhere, including into a new empty route), *swap* (exchange two
  customers of different routes) and *2-opt* (reverse a segment of a route). Only capacity-feasible
  moves are considered; move costs are evaluated in O(1) from the three or four arcs they change.
* **Tabu attributes**: a moved customer may not be moved again; a customer that left a route may not
  re-enter it; a broken arc may not be rebuilt. Tenure = `--tabu` + random(0..4) iterations.
  Because each iteration freezes two or three customers, short tenures (5–10) work best.
* **Aspiration**: a tabu move is accepted when it yields a new best solution.
* **Diversification**: after 50 consecutive non-improving iterations the search restarts from the
  best solution perturbed by `max(3, n/10)` random moves, whose attributes are made tabu so the kick
  cannot be undone right away.
* **Stopping**: `--iterations` consecutive iterations without improving the best solution.

```
s      ← greedy solution;  best ← s;  tabu ← ∅
while iterations without improvement < maxIterations
    m ← best move of N(s) that is not tabu, or that is tabu but improves best (aspiration)
    s ← s ⊕ m;  mark the attributes reversed by m tabu for tenure iterations
    if cost(s) < cost(best): best ← s
    every 50 stagnating iterations: s ← perturb(best)
return best
```

### Ant Colony System

Ant Colony Optimization is inspired by the foraging behaviour of real ants. Artificial ants build
solutions step by step, guided by *pheromone trails* τ(i, j) that summarise how good arc (i, j) was
in previous solutions, and by the *visibility* η(i, j) = 1 / d(i, j).

<img src="./resources/Flowchart-of-ACS.png" width="400" />

The implementation (`acs/AntColonySystemSolver.java`) follows the Ant Colony System of Dorigo &
Gambardella (1997), adapted to the CVRP as in Bullnheimer, Hartl & Strauss, *Applying the Ant System
to the Vehicle Routing Problem*:

* **Construction.** An ant starts at the depot and repeatedly chooses the next customer among the
  unserved ones that still fit in the vehicle; when nothing fits it returns to the depot and starts a
  new route. To mimic "one ant at each customer", ant _k_ is forced to visit customer _k_ first.
* **Transition rule** (pseudo-random proportional). With probability _q<sub>0</sub>_ the ant takes
  the arc maximising τ<sup>α</sup>·η<sup>β</sup> (exploitation); otherwise it draws the next customer
  with probability proportional to τ<sup>α</sup>·η<sup>β</sup> (biased exploration).
* **Local update.** Every traversed arc is immediately updated,
  τ ← (1 − ω)·τ + ω·τ<sub>0</sub>, which makes already used arcs less attractive to the next ants.
* **Local search.** Each ant's routes are improved with 2-opt (`--local-search false` disables it).
* **Global update.** After every iteration, τ ← (1 − ρ)·τ + ρ / L<sub>best</sub> on the arcs of the
  best solution found so far.
* **Initialisation.** τ<sub>0</sub> = 1 / (n · C<sub>nn</sub>) where C<sub>nn</sub> is the cost of the
  greedy solution; one ant per customer; `--iterations` colony iterations.

Runs are reproducible for a given `--seed`. The construction is O(ants · n²) per iteration, so on the
largest instances reduce `--ants` or `--iterations`.

## Parameter tuning with irace

[irace](https://cran.r-project.org/web/packages/irace/vignettes/irace-package.pdf) scenarios for both
metaheuristics live in `tuning/tabu` and `tuning/acs`. They train on `datasets/small` and read the
`Best Value:` line of the program output; the seed irace draws is passed to the program with
`--seed`, so every experiment is reproducible.

```bash
mvn package
cd tuning/tabu          # or tuning/acs
irace                   # needs R with the irace package installed
```

`parameters.txt` describes the search space, `default.txt` the current defaults, `scenario.txt` the
budget. The parameter values tuned for the original implementation (`tabu 7 / iterations 19` and
`alpha 1.95, beta 4.76, rho 0.66, omega 0.85, q0 0.48`) are no longer meaningful: the algorithms
changed substantially in version 2.0 (see below) and `--iterations` now counts non-improving
iterations for the Tabu Search. Re-run irace to tune version 2.0.

## Results

Default parameters, `--seed 1`, real-valued distances, one run per instance, on one core of the CI
container. "Best known" is the CVRPLIB value, computed with *rounded* distances for the A, B and P
sets (so a small difference is expected even for an optimal solution) and with real-valued distances
for Golden_20.

| Instance | Customers | Greedy | ACS (100 it.) | Tabu Search | Best known |
| --- | ---: | ---: | ---: | ---: | ---: |
| A-n32-k5  | 31  | 1146.40 | 823.03  (0.1 s) | **787.08**  (0.2 s) | 784 |
| A-n60-k9  | 59  | 1969.98 | 1555.62 (0.2 s) | **1355.80** (1.0 s) | 1354 |
| A-n80-k10 | 79  | 2255.46 | 2000.89 (0.4 s) | **1812.35** (0.6 s) | 1763 |
| B-n78-k10 | 77  | 1755.03 | 1366.65 (0.3 s) | **1253.54** (1.2 s) | 1221 |
| P-n101-k4 | 100 | 995.80  | 762.66  (0.8 s) | **710.48**  (1.6 s) | 681 |
| Golden_20 | 420 | 2234.21 | 2078.75 (21.2 s) | **2021.17** (12.4 s) | 1820.09 |

For comparison, the original implementation reported 822.86 / 1460.51 / 1833.87 / 1421.52 / 757.24
for the Tabu Search on the five small instances and 863.15 / 1629.55 / 2043.01 / 1479.05 / 883.51
for the ACS, and 2502.39 / 2282.83 on Golden_20. Those numbers are not reproducible (the random generator was not seeded) and some of
them were produced by code with the defects listed below; its Golden_20 figures were computed with
customer 1 mistaken for the depot.

### Conclusions

1. Both metaheuristics improve a lot on the greedy construction; the Tabu Search is clearly the
   stronger of the two on the CVRP and reaches (near-)optimal solutions on the small Augerat
   instances within seconds.
2. A rich neighbourhood (relocate + swap + 2-opt), customer-based tabu attributes, aspiration and a
   perturbation on stagnation are all needed for the Tabu Search to be robust: with only the 1-0
   exchange and arc attributes it stalls in the first local optimum.
3. The ACS depends strongly on its parameters and on the local search applied to the ants' routes.
   As implemented (construction + 2-opt only) it is a decent but not state-of-the-art CVRP solver.
4. There is no guarantee of optimality; results vary with the seed. Report the seed with any result.

## Tests and continuous integration

```bash
mvn test        # 70 unit tests: parser (both dialects, error cases), instance validation,
                # solution feasibility, greedy, 2-opt, tabu search, ACS, command line
mvn verify      # tests + executable jar
```

GitHub Actions (`.github/workflows/ci.yml`) builds the project on JDK 21, runs the tests and smoke-runs
the three algorithms on a small instance.

## Changes in version 2.0

The implementation was reviewed, rewritten around an immutable `VrpInstance` / `Solution` model,
and covered with tests. Defects found in the original code:

* **Tabu Search corrupted vehicle loads.** The demand subtracted from the source route after a move
  was the demand of the *last evaluated* candidate, not of the moved customer, so capacity checks
  drifted and infeasible solutions could be produced (and reported as improvements).
* **Tabu Search skipped most of its neighbourhood.** A `break` instead of `continue` on a tabu move
  abandoned the rest of the target route.
* **Tabu Search could crash or add `Double.MAX_VALUE` to the cost** when every move was tabu, had no
  aspiration criterion, could never open a new route, and used an unseeded `Random`, so irace's
  seeds were ignored and runs were not reproducible.
* **ACS never reset the ant's load** between iterations (`currentCapacity` only grew), so first
  routes became shorter and shorter; the ant placed at the depot built a tour starting `0 -> 0`.
* **Golden / Li instances were read with the wrong depot**: the depot coordinates in
  `DEPOT_SECTION` were ignored and customer 1 was used as the depot.
* The ACS came from the unmaintained `jacof` library pinned to a JitPack `master-SNAPSHOT`
  (non-reproducible builds, one thread per ant, log4j 1.x). It has been reimplemented in this
  repository with the same rules; the project now depends only on Maven Central.
* The greedy solver called `System.exit` from library code; the instance file was parsed twice; the
  parser had dead code for pickup / time-window sections and no error reporting.

Build and tooling: standard Maven layout, Java 21, JCommander 3.0, JUnit 6, maven-shade executable
jar, GitHub Actions CI, `--help`, `--seed`, `--round-distances`, `--ants`, `--local-search`.

## Ideas for further improvement

* **Hybridise ACS with the tabu neighbourhood.** Applying the relocate/swap/2-opt descent (not just
  2-opt) to each ant's solution, or running the Tabu Search from the ACS global-best, is the classic
  way to make ACS competitive on the CVRP.
* **Granular neighbourhoods** (Toth & Vigo): only consider moves that create short arcs
  (e.g. the 20–30 nearest neighbours of each customer). This turns each tabu iteration from O(n²)
  into O(n·k) and is what makes tabu search practical on the 1000-customer X instances.
* **Route-length constraints** for the distance-constrained Golden / Li instances.
* **Or-opt and cross-exchange moves**, and a frequency-based long-term memory (penalising customers
  that move too often) as an alternative to the current random perturbation.
* **Better construction**: Clarke & Wright savings gives a much better starting point than nearest
  neighbour, especially on large instances.
* **Rounded-distance evaluation by default** when the instance belongs to the A/B/P/M/X sets, so
  results are directly comparable to CVRPLIB.

## License

MIT, see [LICENSE](LICENSE).
