import os

from instances import (
    Instance,
    simple_instance,
    two_vehicle_instance,
    larger_instance,
    other_two_vehicle_instance,
    all_vehicles_do_the_same_instance,
    random_points_in_plane_instance
)

INSTANCES = [
    simple_instance(),
    two_vehicle_instance(),
    other_two_vehicle_instance(),
    all_vehicles_do_the_same_instance(),
    larger_instance(),
    random_points_in_plane_instance(3, 1, 1, 1),
    random_points_in_plane_instance(4, 2, 1, 1),
    random_points_in_plane_instance(5, 2, 1, 1),
    random_points_in_plane_instance(6, 3, 2, 1),
    random_points_in_plane_instance(7, 3, 2, 2),
    random_points_in_plane_instance(8, 3, 2, 2),
    random_points_in_plane_instance(9, 4, 2, 2),
    random_points_in_plane_instance(10, 2, 1, 2),
    random_points_in_plane_instance(10, 7, 3, 2),
    random_points_in_plane_instance(11, 2, 1, 2),
    random_points_in_plane_instance(11, 9, 3, 2),
    random_points_in_plane_instance(12, 3, 1, 2),
    random_points_in_plane_instance(12, 10, 3, 2),
    random_points_in_plane_instance(13, 3, 1, 3),
    random_points_in_plane_instance(13, 11, 3, 3),
    random_points_in_plane_instance(14, 4, 1, 3),
    random_points_in_plane_instance(14, 12, 3, 3),
    random_points_in_plane_instance(15, 2, 1, 3),
    random_points_in_plane_instance(15, 8, 2, 3),
    random_points_in_plane_instance(15, 13, 4, 3),
    random_points_in_plane_instance(16, 3, 1, 3),
    random_points_in_plane_instance(16, 8, 2, 3),
    random_points_in_plane_instance(16, 13, 4, 3),
    random_points_in_plane_instance(17, 4, 1, 3),
    random_points_in_plane_instance(17, 9, 2, 3),
    random_points_in_plane_instance(17, 14, 4, 3),
    random_points_in_plane_instance(18, 5, 1, 4),
    random_points_in_plane_instance(18, 10, 2, 4),
    random_points_in_plane_instance(18, 15, 4, 4),
    random_points_in_plane_instance(19, 5, 1, 4),
    random_points_in_plane_instance(19, 10, 2, 4),
    random_points_in_plane_instance(19, 17, 4, 4),
    random_points_in_plane_instance(20, 5, 2, 4),
    random_points_in_plane_instance(20, 10, 3, 4),
    random_points_in_plane_instance(20, 15, 5, 4),
    random_points_in_plane_instance(25, 10, 2, 5),
    random_points_in_plane_instance(25, 15, 3, 5),
    random_points_in_plane_instance(25, 20, 5, 5),
    random_points_in_plane_instance(30, 15, 2, 6),
    random_points_in_plane_instance(30, 20, 3, 6),
    random_points_in_plane_instance(30, 25, 5, 6),
    random_points_in_plane_instance(35, 15, 2, 7),
    random_points_in_plane_instance(35, 20, 3, 7),
    random_points_in_plane_instance(35, 25, 5, 7),
    random_points_in_plane_instance(40, 15, 2, 8),
    random_points_in_plane_instance(40, 20, 3, 8),
    random_points_in_plane_instance(40, 30, 5, 8),
    random_points_in_plane_instance(45, 15, 2, 9),
    random_points_in_plane_instance(45, 20, 3, 9),
    random_points_in_plane_instance(45, 40, 5, 9),
    random_points_in_plane_instance(50, 10, 2, 10),
    random_points_in_plane_instance(50, 20, 3, 10),
    random_points_in_plane_instance(50, 45, 5, 10),
    random_points_in_plane_instance(55, 10, 2, 11),
    random_points_in_plane_instance(55, 20, 3, 11),
    random_points_in_plane_instance(55, 45, 5, 11),
    random_points_in_plane_instance(60, 10, 2, 12),
    random_points_in_plane_instance(60, 20, 3, 12),
    random_points_in_plane_instance(60, 50, 5, 12),
    random_points_in_plane_instance(65, 10, 2, 13),
    random_points_in_plane_instance(65, 20, 3, 13),
    random_points_in_plane_instance(65, 55, 5, 13),
    random_points_in_plane_instance(70, 10, 2, 14),
    random_points_in_plane_instance(70, 30, 3, 14),
    random_points_in_plane_instance(70, 60, 5, 14),
    random_points_in_plane_instance(80, 40, 5, 16),
    random_points_in_plane_instance(90, 45, 5, 18),
    random_points_in_plane_instance(100, 50, 5, 20),
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
                    int(instance.graph[start_node][end_node])
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
