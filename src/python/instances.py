import collections
import random

random.seed(159753)


class Instance:
    def __init__(
            self, name, number_of_vehicles, depot, capacity, graph, packages, neighbors
    ):
        self.name = name
        self.number_of_vehicles = number_of_vehicles
        self.depot = depot
        self.capacity = capacity
        self.graph = graph  # Adjacency Matrix of weights
        self.packages = packages
        self.neighbors = neighbors  # Non-trivial neighbors. Map customer -> neighborhood


def simple_instance():
    return Instance(
        name="simple",
        number_of_vehicles=1,
        depot=1,
        capacity=100,
        graph={
            1: {2: 1, 4: 1},
            2: {1: 1, 3: 1},
            3: {2: 1, 4: 100},
            4: {1: 50, 3: 100},
        },
        packages={
            2: 20,
            3: 20,
        },
        neighbors={},
    )


def two_vehicle_instance():
    # Optimal solution achieved using only one car
    return Instance(
        name="2v1",
        number_of_vehicles=2,
        depot=1,
        capacity=100,
        graph={
            1: {2: 1, 3: 1},
            2: {1: 1, 3: 1},
            3: {1: 1, 4: 100},
            4: {1: 50, 3: 100},
        },
        packages={
            2: 20,
            3: 20,
        },
        neighbors={2: {3}},
    )


def other_two_vehicle_instance():
    # Optimal solution achieved using both cars
    return Instance(
        name="2v2",
        number_of_vehicles=2,
        depot=1,
        capacity=20,
        graph={
            1: {2: 1, 3: 1},
            2: {1: 1, 3: 1},
            3: {1: 1, 4: 2},
            4: {1: 50, 3: 2},
        },
        packages={
            2: 20,
            3: 20,
        },
        neighbors={2: {3}},
    )


def all_vehicles_do_the_same_instance():
    # Optimal solution achieved using both cars doing the same route
    return Instance(
        name="rptd_path",
        number_of_vehicles=2,
        depot=1,
        capacity=30,
        graph={
            1: {2: 1, 3: 100, 4: 50},
            2: {1: 1, 3: 100, 4: 100},
            3: {1: 100, 2: 100, 4: 50},
            4: {1: 100, 2: 100, 3: 100},
        },
        packages={
            3: 20,
            4: 20,
        },
        neighbors={3: {2}, 4: {2}},
    )


def larger_instance():
    return Instance(
        name="large",
        number_of_vehicles=3,
        depot=1,
        capacity=100,
        graph={
            1: {2: 1, 3: 1, 5: 1, 6: 200, 7: 250, 8: 100},
            2: {1: 1, 3: 1, 5: 100, 6: 200, 7: 250},
            3: {1: 1, 4: 100, 6: 1},
            4: {1: 50, 3: 100},
            5: {8: 1, 3: 1, 2: 1, 4: 1, 1: 1},
            6: {7: 1, 4: 200, 1: 200, 3: 200},
            7: {6: 1, 1: 100, 4: 250, 3: 250},
            8: {1: 50, 3: 100, 2: 500, 4: 500, 5: 500, 6: 500},
        },
        packages={
            2: 20,
            3: 20,
            7: 90,
        },
        neighbors={2: {3}, 7: {6}},
    )


def random_instance(size):
    graph = collections.defaultdict(dict)
    neighbors = collections.defaultdict(set)
    for i in range(size):
        for j in range(size):
            random_int = random.randint(1, 100)
            graph[i + 1][j + 1] = random_int
            if random_int < 5 and i >= 1:
                neighbors[i + 1].add(j + 1)
    return Instance(
        name="random_" + str(size),
        number_of_vehicles=int(size / 5) + 2,
        depot=1,
        capacity=100,
        graph=graph,
        packages={i: 20 for i in range(2, size + 1)},
        neighbors=neighbors
    )


def random_instance_many_neighbors(size):
    graph = collections.defaultdict(dict)
    neighbors = collections.defaultdict(set)
    for i in range(size):
        for j in range(size):
            random_int = random.randint(1, 100)
            graph[i + 1][j + 1] = random_int
            if _get_bernoulli_random_value(0.5) and i >= 1:
                neighbors[i + 1].add(j + 1)
    return Instance(
        name="neighbors_" + str(size),
        number_of_vehicles=int(size / 5) + 2,
        depot=1,
        capacity=100,
        graph=graph,
        packages={i: 20 for i in range(2, size + 1)},
        neighbors=neighbors
    )


def _get_horizontal_edge_id(x, y, rows, cols):
    return x * cols + y + 1


def _get_vertical_edge_id(x, y, rows, cols):
    shift = (rows + 1) * cols
    return shift + x * (cols + 1) + y + 1


def _get_node_id(x, y, rows, cols):
    return x * (cols + 1) + y + 1


def _get_grid_graph(rows, cols):
    graph = collections.defaultdict(dict)
    for i in range(rows):
        for j in range(cols):
            current_node = i * cols + j + 1
            if j % cols != cols - 1:
                graph[current_node][current_node + 1] = 1
            if j % cols != 0:
                graph[current_node][current_node - 1] = 1
            if i % rows != rows - 1:
                graph[current_node][current_node + cols] = 1
            if i % rows != 0:
                graph[current_node][current_node - cols] = 1
    return graph


def _get_bernoulli_random_value(prob):
    return random.choices([0, 1], weights=[1 - prob, prob])[0]


def _get_random_grid_customers(rows, cols, prob):
    customers = {}
    for i in range(rows):
        for j in range(cols):
            bernoulli = _get_bernoulli_random_value(prob)
            current_node = i * cols + j + 1
            if bernoulli == 1 and j % cols != cols - 1:
                customers[current_node] = 10
    return customers


def get_base_grid_instance(rows, cols, vehicles, customer_prob=0.1, capacity=0):
    """
    returns an instance of M x N Grid city
    M rows, N columns
    M x N nodes
    """
    graph = _get_grid_graph(rows, cols)
    packages = _get_random_grid_customers(rows, cols, customer_prob)
    if not capacity:
        capacity = len(packages) * 10
    return Instance(
        name="_grid_" + str(rows) + "_" + str(cols),
        number_of_vehicles=vehicles,
        depot=1,
        capacity=capacity,
        graph=graph,
        packages=packages,
        neighbors={}
    )
