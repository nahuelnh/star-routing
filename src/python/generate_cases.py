import os

from instances import (
    Instance,
    simple_instance,
    two_vehicle_instance,
    larger_instance,
    random_instance,
    other_two_vehicle_instance,
    random_instance_many_neighbors,
    all_vehicles_do_the_same_instance
)

INSTANCES = [
    simple_instance(),
    two_vehicle_instance(),
    other_two_vehicle_instance(),
    all_vehicles_do_the_same_instance(),
    larger_instance(),
    random_instance(40),
    random_instance(20),
    random_instance_many_neighbors(20),
    random_instance_many_neighbors(40),
]


def _dir_naming_convention(instance_name: str):
    return "../resources/instance_{}/".format(instance_name)


def generate_cases():
    for instance in INSTANCES:
        write_to_files(instance)


def _write_params_file(instance: Instance, output_dir: str):
    with open(output_dir + "params.txt", "w") as params_file:
        params_file.write("vehicles " + str(instance.number_of_vehicles) + "\n")
        params_file.write("depot " + str(instance.depot) + "\n")
        params_file.write("capacity " + str(instance.capacity) + "\n")


def _write_packages_file(instance: Instance, output_dir: str):
    with open(output_dir + "packages.txt", "w") as packages_file:
        for customer in instance.packages:
            package_weight = instance.packages[customer]
            packages_file.write(str(customer) + " " + str(package_weight) + "\n")


def _write_customers_file(instance: Instance, output_dir: str):
    with open(output_dir + "neighbors.txt", "w") as customers_file:
        for customer in instance.neighbors:
            neighborhood = instance.neighbors[customer]
            for neighbor in neighborhood:
                customers_file.write(str(customer) + " " + str(neighbor) + "\n")


def _write_graph_file(instance: Instance, output_dir: str):
    with open(output_dir + "graph.txt", "w") as graph_file:
        number_of_vertices = max(v for v in instance.graph.keys())
        for start_node in range(1, number_of_vertices + 1):
            for end_node in range(1, number_of_vertices + 1):
                weight_or_negative = (
                    instance.graph[start_node][end_node]
                    if start_node in instance.graph and end_node in instance.graph[start_node]
                    else -1
                )
                graph_file.write(str(start_node) + " " + str(end_node) + " " + str(weight_or_negative) + "\n")


def write_to_files(instance: Instance):
    output_dir = _dir_naming_convention(instance.name)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    _write_params_file(instance, output_dir)
    _write_packages_file(instance, output_dir)
    _write_customers_file(instance, output_dir)
    _write_graph_file(instance, output_dir)


if __name__ == "__main__":
    generate_cases()
