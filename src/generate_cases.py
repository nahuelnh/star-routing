import os
from instances import (
    Instance,
    simple_instance,
)


INSTANCES = [
    simple_instance(),
]


def dir_naming_convention(instance_name):
    return "src/resources/instance{}/".format(instance_name)


def generate_cases():
    for instance in INSTANCES:
        write_to_files(instance)


def write_to_files(instance: Instance):

    output_dir = dir_naming_convention(instance.name)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    with open(output_dir + "params.txt", "w") as params_file:
        params_file.write("vehicles " + str(instance.number_of_vehicles) + "\n")
        params_file.write("depot " + str(instance.depot) + "\n")
        params_file.write("capacity " + str(instance.capacity) + "\n")

    with open(output_dir + "packages.txt", "w") as packages_file:
        for idx, customer in enumerate(instance.packages):
            edge_start, edge_end = customer[0], customer[1]
            package_weight = instance.packages[customer]
            packages_file.write(
                str(idx)
                + " "
                + str(edge_start)
                + " "
                + str(edge_end)
                + " "
                + str(package_weight)
                + "\n"
            )

    with open(output_dir + "graph.txt", "w") as graph_file:
        number_of_vertices = max(v for v in instance.graph.keys())
        for start_node in range(1, number_of_vertices + 1):
            for end_node in range(1, number_of_vertices + 1):
                weight_or_zero = (
                    instance.graph[start_node][end_node]
                    if start_node in instance.graph
                    and end_node in instance.graph[start_node]
                    else 0
                )
                graph_file.write(
                    str(start_node)
                    + " "
                    + str(end_node)
                    + " "
                    + str(weight_or_zero)
                    + "\n"
                )


if __name__ == "__main__":
    generate_cases()
