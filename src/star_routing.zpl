param dir := "resources/instance4/";


#===============================================================================
### Sets ###
#===============================================================================

set Edges := {read dir + "graph.txt" as "<1n>"};
set Vertices := {read dir + "graph.txt" as "<2n>"};
set Customers := {read dir + "packages.txt" as "<1n>"};	# Edges with customers
set InputParamLabels := {read dir + "params.txt" as "<1s>"};


#===============================================================================
### Parameters ###
#===============================================================================

param inputParams[InputParamLabels] := read dir + "params.txt" as "<1s> 2n";
param graph[Edges * Vertices] := read dir + "graph.txt" as "<1n, 2n> 3n";	# Graph represented as Incidence Matrix
param weight[Edges] := read dir + "weights.txt" as "<1n> 2n"; 			# Weights on the edges (modelling traffic)
param first := inputParams["first"];						# Departure node
param last := inputParams["last"];						# Destination node 
param capacity := inputParams["capacity"];					# Vehicle capacity
param numberOfVehicles := inputParams["vehicles"];				# Number of Vehicles 
set Vehicles := {1 to numberOfVehicles};					# Set of Vehicles
param volume[Customers] := read dir + "packages.txt" as "<1n> 2n";		# Volume of packages to be delivered
param M := card(Edges) + 100;							# Large enough constant


#===============================================================================
### Variables ###
#===============================================================================

var x[Edges * Vehicles] binary;			# Whether edge e is part of the optimal path of vehicle v
var y[Edges * Vehicles] binary;			# Whether customers on edge e are served by vehicle v  
var degree[Vertices * Vehicles] integer >= 0;	# Redundant, equals to the vertex degree in the path (not in the graph)
var theta[Vertices * Vehicles] binary;		# Aux to check inequality restriction 


#===============================================================================
### Goal ###
#===============================================================================

minimize cost: 
	sum <e, v> in Edges * Vehicles: weight[e] * x[e, v];	# Minimum weight path


#===============================================================================
### Restrictions ###
#===============================================================================

# Every edge in Customers should be covered by exactly one vehicle:
subto r1: 
	forall <c> in Customers do
		(sum <v> in Vehicles: y[c, v]) == 1;

# A vehicle can only serve visited edges 
# This model permits serving edges with no customers, 
# as it does not change the optimal solution
subto r2:
	forall <e> in Edges do
		forall <v> in Vehicles do
			y[e, v] <= x[e, v];

# Every vehicle should meet the capacity restrictions
subto r3:
	forall <v> in Vehicles do
		(sum <c> in Customers: y[c, v] * volume[c]) <= capacity;

# The chosen set of edges x[e] should be a path, i.e.:
# Every node of the path has degree <= 2, and only the start and end have degree == 1.

# Set the degree of the vertex in the path
# This restriction is only used for readability purposes
subto r4: 
	forall <vertex> in Vertices do
		forall <vehicle> in Vehicles do
			(sum <e> in Edges with graph[e, vertex] != 0: x[e, vehicle]) == degree[vertex, vehicle];

# Every node of every path has degree <=2
subto r5: 
	forall <vertex> in Vertices do
		forall <vehicle> in Vehicles do
			degree[vertex, vehicle] <= 2;

# All paths start at departure node
subto r6:
	forall <v> in Vehicles do
		degree[first, v] == 1;

# All paths end at destination node
subto r7:
	forall <v> in Vehicles do
		degree[last, v] == 1;
	
# Every vertex has degree != 1, except for the start and end
subto r8:
	forall <vertex> in Vertices with vertex != first and vertex != last do
		forall <vehicle> in Vehicles do
			degree[vertex, vehicle] >= 2 - M * theta[vertex, vehicle];

subto r9:
	forall <vertex> in Vertices with vertex != first and vertex != last do
		forall <vehicle> in Vehicles do
			degree[vertex, vehicle] <= 0 + M * (1 - theta[vertex, vehicle]);
