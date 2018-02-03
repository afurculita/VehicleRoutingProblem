# Vehicle Routing Problem

Vehicle Routing Problem or simply VRP is a well known combinatorial optimization problem and a generalization of the 
travelling salesman problem. A definition of the problem is this: We have a number of customers that have a demand for 
a delivery. Which are the optimal (minimal) routes for a fleet of vehicles starting from a single point (depot) to 
deliver the requested goods in all customers. Finding optimal solution is a NP-hard problem so heuristic strategies 
are proposed for approximation of the optimal solution. For more about the problem see: 
https://en.wikipedia.org/wiki/Vehicle_routing_problem.

The variant of VRP that we are solving here is *Capacitated Vehicle Routing Problem (CVRP)*. It can formally be defined as 
follows:
  
  Let _G = (V, A)_ be a graph where _V_ is the vertex set and _A_ is the arc set.  One of the vertices represents 
  the depot at which a fleet of m identical vehicles of capacity _Q_ is based, and the other vertices customers 
  that need to be serviced.  With each customer vertex vi are associated a demand _qi_.  With each arc _(vi, vj)_ of _A_ 
  are associated a cost _cij_.  The _CVRP_ consists in finding a set of routes such that:
  
1. Each route begins and ends at the depot;
2. Each customer is visited exactly once by exactly one route;
3. The total demand of the customers assigned to each route does not exceed Q;
4. The total cost of the routes is minimized.

## Algorithms for solving VRP

To solve VRP we are using the following metaheuristic algorithms: *Tabu Search* with the initial solution generated 
with a Greedy Algorithm and *Ant Colony System*.

### Tabu Search

Tabu search is a metaheuristic search method employing local search methods. Local (neighborhood) searches take a 
potential solution to a problem and check its immediate neighbors (that is, solutions that are similar except for 
very few minor details) in the hope of finding an improved solution. Local search methods have a tendency to 
become stuck in suboptimal regions or on plateaus where many solutions are equally fit.

Tabu search enhances the performance of local search by relaxing its basic rule. First, at each step worsening moves 
can be accepted if no improving move is available (like when the search is stuck at a strict local minimum). 
In addition, prohibitions (henceforth the term tabu) are introduced to discourage the search from coming back 
to previously-visited solutions.

The following chart describes the steps taken in the tabu search algorithm:

<img src="./resources/Flow-chart-of-tabu-search-algorithm.png" width="400" />

The pseudo-code of the Tabu search algorithm:

         1 sBest ← s0
         2 bestCandidate ← s0
         3 tabuList ← []
         4 tabuList.push(s0)
         5 while (not stoppingCondition())
         6 	sNeighborhood ← getNeighbors(bestCandidate)
         7 	bestCandidate ← sNeighborHood.firstElement
         8 	for (sCandidate in sNeighborHood)
         9 		if ( (not tabuList.contains(sCandidate)) and (fitness(sCandidate) > fitness(bestCandidate)) )
        10 			bestCandidate ← sCandidate
        11 		end
        12 	end
        13 	if (fitness(bestCandidate) > fitness(sBest))
        14 		sBest ← bestCandidate
        15 	end
        16 	tabuList.push(bestCandidate)
        17 	if (tabuList.size > maxTabuSize)
        18 		tabuList.removeFirst()
        19 	end
        20 end
        21 return sBest

### Ant Colony System

_Ant Colony System (ACS)_ is an algorithmic approach inspired by the foraging behavior of real ants. Artificial ants
 are used to construct a solution for the problem by using the pheromone information from previously generated solutions.
 
 The following flowchart describes the ACS algorithm steps:
 
<img src="./resources/Flowchart-of-ACS.png" width="400" />

