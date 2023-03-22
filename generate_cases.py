import os

OUTPUT_DIR = "resources/"


class Example:
    def __init__(
        self, number_of_vehicles, first, last, capacity, graph, packages, weights
    ):
        self.number_of_vehicles = number_of_vehicles
        self.first = first
        self.last = last
        self.capacity = capacity
        # Represented as a dict: edge -> incident_nodes
        self.graph = graph
        self.packages = packages
        self.weights = weights


def example1():
    return Example(
        number_of_vehicles=3,
        first=2,
        last=3,
        capacity=100,
        graph={
            1: [1, 2],
            2: [2, 3],
            3: [1, 3],
        },
        packages={
            1: 20,
            2: 81,
            3: 20,
        },
        weights={
            1: 1,
            2: 100,
            3: 1,
        },
    )


def example2():
    """
    M x N Grid city
    """
    M = 5
    N = 5
    graph = {}
    for i in range(M):
        for j in range(N):
            graph[i * N + j + 1] = [i * (N + 1) + j + 1, i * (N + 1) + j + 2]
            graph[M * N + i * N + j + 1] = [
                i * (N + 1) + j + 1,
                (i + 1) * (N + 1) + j + 1,
            ]

    return Example(
        number_of_vehicles=2,
        first=1,
        last=20,
        capacity=100,
        graph=graph,
        packages={
            1: 20,
            2: 20,
            3: 20,
            4: 20,
            8: 20,
            9: 20,
        },
        weights={i: 1 for i in range(1, 2 * M * N + 1)},
    )


EXAMPLES = [
    example1(),
    example2(),
]


def generate_cases():
    for i, ex in enumerate(EXAMPLES):
        output_dir = OUTPUT_DIR + "ejemplo{}/".format(i + 1)
        write_to_file(output_dir, ex)


def write_to_file(output_dir, example: Example):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    with open(output_dir + "params.txt", "w") as params_file:
        params_file.write("vehicles " + str(example.number_of_vehicles) + "\n")
        params_file.write("first " + str(example.first) + "\n")
        params_file.write("last " + str(example.last) + "\n")
        params_file.write("capacity " + str(example.capacity) + "\n")

    with open(output_dir + "packages.txt", "w") as packages_file:
        for customer, package_weight in example.packages.items():
            packages_file.write(str(customer) + " " + str(package_weight) + "\n")

    with open(output_dir + "weights.txt", "w") as weights_file:
        for edge, weight in example.weights.items():
            weights_file.write(str(edge) + " " + str(weight) + "\n")

    with open(output_dir + "graph.txt", "w") as graph_file:
        number_of_edges = max(example.graph.keys())
        number_of_vertices = max(max(v) for v in example.graph.values())
        for e in range(1, number_of_edges + 1):
            for v in range(1, number_of_vertices + 1):
                is_incident = 1 if v in example.graph[e] else 0
                graph_file.write(str(e) + " " + str(v) + " " + str(is_incident) + "\n")


if __name__ == "__main__":
    generate_cases()
