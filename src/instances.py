from collections import defaultdict
import random

random.seed(123456)


class Instance:
    def __init__(self, name, number_of_vehicles, depot, capacity, graph, packages):
        self.name = name
        self.number_of_vehicles = number_of_vehicles
        self.depot = depot
        self.capacity = capacity
        self.graph = graph  # Adjacency List with Weights
        self.packages = packages


def simple_instance():
    return Instance(
        name="1",
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
            (1, 2): 20,
            (3, 4): 20,
        },
    )


def _get_horizontal_edge_id(x, y, rows, cols):
    return x * cols + y + 1


def _get_vertical_edge_id(x, y, rows, cols):
    shift = (rows + 1) * cols
    return shift + x * (cols + 1) + y + 1


def _get_node_id(x, y, rows, cols):
    return x * (cols + 1) + y + 1


def _get_grid(rows, cols):
    graph = defaultdict(dict)
    max_node = rows * cols
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


def _get_random_customers(rows, cols, prob):
    customers = {}
    for i in range(rows):
        for j in range(cols):
            bernoulli = random.choices([0, 1], weights=[1 - prob, prob])[0]
            current_node = i * cols + j + 1
            if bernoulli == 1 and j % cols != cols - 1:
                customers[(current_node, current_node + 1)] = 10
    return customers


def get_base_grid_instance(rows, cols, vehicles, customer_prob=0.1, capacity=0):
    """
    returns an instance of M x N Grid city
    M rows, N columns
    M x N nodes
    """
    graph = _get_grid(rows, cols)
    packages = _get_random_customers(rows, cols, customer_prob)
    if not capacity:
        capacity = len(packages) * 10
    return Instance(
        name="2",
        number_of_vehicles=vehicles,
        depot=1,
        capacity=capacity,
        graph=graph,
        packages=packages,
    )


def tanslate_from_tagliavini(instance_name, path_to_src):
    """
    translate instance from the format in Tagliavini's Msc Thesis
    to a format readable by this script
    """

    packages = {}
    with open(path_to_src, "r") as source_file:
        rows, cols, _ = source_file.readline().split(" ")
        rows = int(rows)
        cols = int(cols)
        for line in source_file:
            x1, y1, x2, y2 = line.split(" ")
            x1 = int(x1)
            x2 = int(x2)
            y1 = int(y1)
            y2 = int(y2)
            edge = 0
            if x1 < x2 and y1 == y2:
                edge = _get_horizontal_edge_id(x1, y1, rows, cols)
            if x2 < x1 and y1 == y2:
                edge = _get_horizontal_edge_id(x2, y2, rows, cols)
            if y1 < y2 and x1 == x2:
                edge = _get_vertical_edge_id(x1, y1, rows, cols)
            if y2 < y1 and x1 == x2:
                edge = _get_vertical_edge_id(x2, y2, rows, cols)
            if edge:
                packages[edge] = 1

    graph = _get_grid(rows, cols)
    total_edges = (rows + 1) * cols + rows * (cols + 1)
    total_nodes = (rows + 1) * (cols + 1)
    return Instance(
        name=instance_name,
        number_of_vehicles=2,
        first=1,
        last=total_nodes,
        capacity=total_edges,
        graph=graph,
        packages=packages,
        weights={i: 1 for i in range(1, total_edges + 1)},
    )
