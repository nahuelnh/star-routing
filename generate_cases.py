OUTPUT_DIR = "resources/"


class Example:

    def __init__(self, number_of_vehicles, first, last, capacity, graph, packages, weights):
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
        }
    )


EXAMPLES = [
    example1(),
]


def generate_cases():
    for e in EXAMPLES:
        write_to_file(e)


def write_to_file(example: Example):
    with open(OUTPUT_DIR + "params.txt", 'w') as params_file:
        params_file.write("vehicles " + str(example.number_of_vehicles) + "\n")
        params_file.write("first " + str(example.first) + "\n")
        params_file.write("last " + str(example.last) + "\n")
        params_file.write("capacity " + str(example.capacity) + "\n")

    with open(OUTPUT_DIR + "packages.txt", 'w') as packages_file:
        for customer, package_weight in example.packages.items():
            packages_file.write(str(customer) + " " +
                                str(package_weight) + "\n")

    with open(OUTPUT_DIR + "weights.txt", 'w') as weights_file:
        for edge, weight in example.weights.items():
            weights_file.write(str(edge) + " " + str(weight) + "\n")

    with open(OUTPUT_DIR + "graph.txt", 'w') as graph_file:
        number_of_edges = max(example.graph.keys())
        number_of_vertices = max(max(v) for v in example.graph.values())
        for e in range(1, number_of_edges + 1):
            for v in range(1, number_of_vertices + 1):
                is_incident = 1 if v in example.graph[e] else 0
                graph_file.write(str(e) + " " + str(v) +
                                 " " + str(is_incident) + "\n")


if __name__ == '__main__':
    generate_cases()
