param dir := "resources/instance2/";


#===============================================================================
### Sets ###
#===============================================================================

set Nodes := {read dir + "graph.txt" as "<1n>"};
set CustomerIndices := {read dir + "packages.txt" as "<1n>"};
set InputParamLabels := {read dir + "params.txt" as "<1s>"};

#===============================================================================
### Parameters ###
#===============================================================================

param inputParams[InputParamLabels] := read dir + "params.txt" as "<1s> 2n";	# Parameters
param capacity := inputParams["capacity"];					# Vehicle capacity
param numberOfVehicles := inputParams["vehicles"];				# Number of Vehicles 
param Depot := inputParams["depot"];						# Depot node is fixed

param graph[Nodes * Nodes] := read dir + "graph.txt" as "<1n, 2n> 3n";		# Graph represented as Adjacency Matrix

set Customers := {read dir + "packages.txt" as "<1n, 2n, 3n>"};			# Set of customers as a tuple <customer_id, edge_start, edge_end>

set Vehicles := {1 to numberOfVehicles};					# Set of Vehicles

param volume[CustomerIndices] := read dir + "packages.txt" as "<1n> 4n";	# Size of packages to be delivered

param numberOfNodes := card(Nodes);
param bigM := numberOfNodes * (numberOfNodes - 1);

#===============================================================================
### Variables ###
#===============================================================================

var x[Nodes * Nodes * Vehicles] binary;		    # Whether edge (i, j) is part of the optimal path of vehicle k
var y[CustomerIndices * Vehicles] binary;	    # Whether customer c is served by vehicle k  
var u[Nodes * Nodes * Vehicles] integer >= 0;	# Order in which vertex is visited, only used for MTZ conditions 

#===============================================================================
### Goal ###
#===============================================================================

minimize cost: 
	sum <i, j> in Nodes * Nodes: 
		graph[i, j] * (sum <k> in Vehicles: x[i, j, k]);	# Minimum weight path


#===============================================================================
### Constraints ###
#===============================================================================


# Vehicle leaves the node that it enters
subto r1:
	forall <i> in Nodes:
		forall <k> in Vehicles:
			(sum <j> in Nodes with graph[i, j] != 0: x[i, j, k]) == (sum <j> in Nodes with graph[j, i] != 0: x[j, i, k]);

# Every customer is served by exactly one vehicle
subto r2:
	forall <customerId, edgeStart, edgeEnd> in Customers:
		(sum <k> in Vehicles: y[customerId, k]) == 1;

# Every vehicle leaves the depot
subto r3:
	forall <k> in Vehicles:
		(sum <j> in Nodes with graph[Depot, j] != 0: x[Depot, j, k]) == 1;

# A vehicle can only serve visited customers 
subto r4:
	forall <customerId, edgeStart, edgeEnd> in Customers:
		forall <k> in Vehicles:
			y[customerId, k] <= (sum <i> in Nodes: (x[i, edgeStart, k] + x[i, edgeEnd, k]));

# Capacity constraint
subto r5: 
	forall <k> in Vehicles:
		(sum <customerId, edgeStart, edgeEnd> in Customers: volume[customerId] * y[customerId, k]) <= capacity;

# Subtour Elimination Constraints (SEC)
subto r6:
	forall <i> in Nodes with i != Depot:
		forall <j> in Nodes with j != Depot and j != i:
        	forall <k> in Nodes with k != Depot and k != j:
			    forall <v> in Vehicles:
				    u[i, j, v] - u[j, k, v] + bigM * (x[i, j, v] + x[j, k, v]) <= 2 * bigM - 1;

subto r7:
	forall <i> in Nodes:
    	forall <j> in Nodes:
		    forall <v> in Vehicles:
			    1 <= u[i, j, v] <= bigM;

# Non existent edges should not be added to the path
# Redundant constraint
subto r8:
	forall <i> in Nodes:
		forall <j> in Nodes:
			forall <k> in Vehicles:
				x[i, j, k] <= graph[i,j];
				
# Every node is entered at most once, i.e. 
# the path contains no inner cicles
# Redundant inequality
#subto r9:
#	forall <i> in Nodes:
#		forall <k> in Vehicles:
#			(sum <j> in Nodes with graph[i, j] != 0: x[i, j, k]) <= 1;
