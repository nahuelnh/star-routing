param dir := "resources/instance1/";


#===============================================================================
### Sets ###
#===============================================================================

set Vertices := {read dir + "graph.txt" as "<1n>"};
set CustomerIndices := {read dir + "packages.txt" as "<1n>"};
set InputParamLabels := {read dir + "params.txt" as "<1s>"};

#===============================================================================
### Parameters ###
#===============================================================================

param inputParams[InputParamLabels] := read dir + "params.txt" as "<1s> 2n";
param graph[Vertices * Vertices] := read dir + "graph.txt" as "<1n, 2n> 3n";	# Graph represented as Adjacency Matrix
set Customers := {read dir + "packages.txt" as "<1n, 2n, 3n>"};			# Set of customers as a tuple <customer_id, start_node, end_node>
param capacity := inputParams["capacity"];					# Vehicle capacity
param numberOfVehicles := inputParams["vehicles"];				# Number of Vehicles 
set Vehicles := {1 to numberOfVehicles};					# Set of Vehicles
param volume[CustomerIndices] := read dir + "packages.txt" as "<1n> 2n";	# Volume of packages to be delivered
param Depot := inputParams["depot"];						# Depot node is fixed

#===============================================================================
### Variables ###
#===============================================================================

var x[Vertices * Vertices * Vehicles] binary;	# Whether edge (i, j) is part of the optimal path of vehicle k
var y[CustomerIndices * Vehicles] binary;	# Whether customer c is served by vehicle k  
var u[Vertices * Vehicles] integer >= 0;	# Order in which vertex is visited, only used for MTZ conditions 

#===============================================================================
### Goal ###
#===============================================================================

minimize cost: 
	sum <i, j> in Vertices * Vertices: 
		graph[i, j] * (sum <k> in Vehicles: x[i, j, k]);	# Minimum weight path


#===============================================================================
### Constraints ###
#===============================================================================


# Vehicle leaves the node that it enters
subto r1:
	forall <i> in Vertices:
		forall <k> in Vehicles:
			(sum <j> in Vertices with graph[i, j] != 0: x[i, j, k]) == (sum <j> in Vertices with graph[j, i] != 0: x[j, i, k]);

# Every customer is served by exactly one vehicle
subto r2:
	forall <customerId, edgeStart, edgeEnd> in Customers:
		(sum <k> in Vehicles: y[customerId, k]) == 1;

# Every node leaves the depot
subto r3:
	forall <k> in Vehicles:
		(sum <j> in Vertices with graph[Depot, j] != 0: x[Depot, j, k]) == 1;

# A vehicle can only serve visited customers 
subto r4:
	forall <customerId, edgeStart, edgeEnd> in Customers:
		forall <k> in Vehicles:
			y[customerId, k] <= (sum <i> in Vertices: (x[i, edgeStart, k] + x[i, edgeEnd, k]));

# Capacity constraint
subto r5: 
	forall <k> in Vehicles:
		(sum <customerId, edgeStart, edgeEnd> in Customers: volume[customerId] * y[customerId, k]) <= capacity;

# Subtour Elimination Constraints (SEC)
subto r6:
	forall <i> in Vertices:
		forall <j> in Vertices:
			forall <k> in Vehicles:
				u[j, k] - u[i, k] >= capacity * (1 - x[i, j, k]);

subto r7:
	forall <i> in Vertices:
		forall <k> in Vehicles:
			volume[i] <= u[i, k] <= capacity;


