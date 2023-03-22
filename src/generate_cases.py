import os
from instances import (
    Instance,
    simple_instance,
    get_base_grid_instance,
    tanslate_from_tagliavini,
)


INSTANCES = [
    simple_instance(),
    get_base_grid_instance(rows=10, cols=10),
    tanslate_from_tagliavini("../MSc-Thesis/src/instance/5_40.in"),
    tanslate_from_tagliavini("../MSc-Thesis/src/instance/15_20.in"),
]


def dir_naming_convention(idx):
    return "src/resources/instance{}/".format(idx + 1)


def generate_cases():
    for idx, instance in enumerate(INSTANCES):
        write_to_files(dir_naming_convention(idx), instance)


def write_to_files(output_dir, instance: Instance):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    with open(output_dir + "params.txt", "w") as params_file:
        params_file.write("vehicles " + str(instance.number_of_vehicles) + "\n")
        params_file.write("first " + str(instance.first) + "\n")
        params_file.write("last " + str(instance.last) + "\n")
        params_file.write("capacity " + str(instance.capacity) + "\n")

    with open(output_dir + "packages.txt", "w") as packages_file:
        for customer, package_weight in instance.packages.items():
            packages_file.write(str(customer) + " " + str(package_weight) + "\n")

    with open(output_dir + "weights.txt", "w") as weights_file:
        for edge, weight in instance.weights.items():
            weights_file.write(str(edge) + " " + str(weight) + "\n")

    with open(output_dir + "graph.txt", "w") as graph_file:
        number_of_edges = max(instance.graph.keys())
        number_of_vertices = max(max(v) for v in instance.graph.values())
        for e in range(1, number_of_edges + 1):
            for v in range(1, number_of_vertices + 1):
                is_incident = 1 if v in instance.graph[e] else 0
                graph_file.write(str(e) + " " + str(v) + " " + str(is_incident) + "\n")


if __name__ == "__main__":
    generate_cases()
