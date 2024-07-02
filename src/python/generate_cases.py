import os

from instances import (
    Instance,
    simple_instance,
    two_vehicle_instance,
    larger_instance,
    other_two_vehicle_instance,
    all_vehicles_do_the_same_instance,
    random_points_in_plane_instance,
)

INSTANCES = [
    random_points_in_plane_instance(10, 2, 2, 2),
    random_points_in_plane_instance(10, 5, 3, 2),
    random_points_in_plane_instance(10, 7, 5, 2),
    random_points_in_plane_instance(11, 2, 2, 2),
    random_points_in_plane_instance(11, 5, 3, 2),
    random_points_in_plane_instance(11, 8, 5, 2),
    random_points_in_plane_instance(12, 3, 2, 2),
    random_points_in_plane_instance(12, 6, 3, 2),
    random_points_in_plane_instance(12, 9, 5, 2),
    random_points_in_plane_instance(13, 3, 2, 2),
    random_points_in_plane_instance(13, 6, 3, 2),
    random_points_in_plane_instance(13, 9, 5, 2),
    random_points_in_plane_instance(14, 3, 2, 2),
    random_points_in_plane_instance(14, 7, 3, 2),
    random_points_in_plane_instance(14, 10, 5, 2),
    random_points_in_plane_instance(15, 3, 2, 3),
    random_points_in_plane_instance(15, 7, 3, 3),
    random_points_in_plane_instance(15, 11, 5, 3),
    random_points_in_plane_instance(16, 4, 2, 3),
    random_points_in_plane_instance(16, 8, 3, 3),
    random_points_in_plane_instance(16, 12, 5, 3),
    random_points_in_plane_instance(17, 4, 2, 3),
    random_points_in_plane_instance(17, 8, 3, 3),
    random_points_in_plane_instance(17, 12, 5, 3),
    random_points_in_plane_instance(18, 4, 2, 3),
    random_points_in_plane_instance(18, 9, 3, 3),
    random_points_in_plane_instance(18, 13, 5, 3),
    random_points_in_plane_instance(19, 4, 2, 3),
    random_points_in_plane_instance(19, 9, 3, 3),
    random_points_in_plane_instance(19, 14, 5, 3),
    random_points_in_plane_instance(20, 5, 2, 4),
    random_points_in_plane_instance(20, 10, 3, 4),
    random_points_in_plane_instance(20, 15, 5, 4),
    random_points_in_plane_instance(21, 5, 2, 4),
    random_points_in_plane_instance(21, 10, 3, 4),
    random_points_in_plane_instance(21, 15, 5, 4),
    random_points_in_plane_instance(22, 5, 2, 4),
    random_points_in_plane_instance(22, 11, 3, 4),
    random_points_in_plane_instance(22, 16, 5, 4),
    random_points_in_plane_instance(23, 5, 2, 4),
    random_points_in_plane_instance(23, 11, 3, 4),
    random_points_in_plane_instance(23, 17, 5, 4),
    random_points_in_plane_instance(24, 6, 2, 4),
    random_points_in_plane_instance(24, 12, 3, 4),
    random_points_in_plane_instance(24, 18, 5, 4),
    random_points_in_plane_instance(25, 6, 2, 5),
    random_points_in_plane_instance(25, 12, 3, 5),
    random_points_in_plane_instance(25, 18, 5, 5),
    random_points_in_plane_instance(26, 6, 2, 5),
    random_points_in_plane_instance(26, 13, 3, 5),
    random_points_in_plane_instance(26, 19, 5, 5),
    random_points_in_plane_instance(27, 6, 2, 5),
    random_points_in_plane_instance(27, 13, 3, 5),
    random_points_in_plane_instance(27, 20, 5, 5),
    random_points_in_plane_instance(28, 7, 2, 5),
    random_points_in_plane_instance(28, 14, 3, 5),
    random_points_in_plane_instance(28, 21, 5, 5),
    random_points_in_plane_instance(29, 7, 2, 5),
    random_points_in_plane_instance(29, 14, 3, 5),
    random_points_in_plane_instance(29, 21, 5, 5),
    random_points_in_plane_instance(30, 7, 2, 6),
    random_points_in_plane_instance(30, 15, 3, 6),
    random_points_in_plane_instance(30, 22, 5, 6),
    random_points_in_plane_instance(32, 8, 2, 6, 2),
    random_points_in_plane_instance(32, 16, 3, 6, 2),
    random_points_in_plane_instance(34, 8, 2, 6),
    random_points_in_plane_instance(34, 17, 3, 6),
    random_points_in_plane_instance(36, 9, 2, 7),
    random_points_in_plane_instance(36, 18, 3, 7),
    random_points_in_plane_instance(38, 9, 2, 7, 2),
    random_points_in_plane_instance(38, 19, 3, 7),
    random_points_in_plane_instance(40, 10, 2, 8),
    random_points_in_plane_instance(40, 20, 3, 8, 5),
    random_points_in_plane_instance(42, 10, 2, 8, 15),
    random_points_in_plane_instance(42, 21, 3, 8),
    random_points_in_plane_instance(44, 11, 2, 8),
    random_points_in_plane_instance(44, 22, 3, 8, 2),
    random_points_in_plane_instance(46, 11, 2, 9),
    random_points_in_plane_instance(46, 23, 3, 9),
    random_points_in_plane_instance(48, 12, 2, 9, 2),
    random_points_in_plane_instance(48, 24, 3, 9),
    random_points_in_plane_instance(50, 12, 2, 10, 2),
    random_points_in_plane_instance(50, 25, 3, 10),
    random_points_in_plane_instance(52, 13, 2, 10, 2),
    random_points_in_plane_instance(52, 26, 3, 10),
    random_points_in_plane_instance(54, 13, 2, 10, 2),
    random_points_in_plane_instance(54, 27, 3, 10),
    random_points_in_plane_instance(56, 14, 2, 11),
    random_points_in_plane_instance(56, 28, 3, 11),
    random_points_in_plane_instance(58, 14, 2, 11, 2),
    random_points_in_plane_instance(58, 29, 3, 11),
    random_points_in_plane_instance(60, 15, 2, 12),
    random_points_in_plane_instance(60, 30, 3, 12),
    random_points_in_plane_instance(62, 15, 2, 12),
    random_points_in_plane_instance(62, 31, 3, 12, 2),
    random_points_in_plane_instance(64, 16, 2, 12),
    random_points_in_plane_instance(64, 32, 3, 12),
    random_points_in_plane_instance(66, 16, 2, 13),
    random_points_in_plane_instance(66, 33, 3, 13),
    random_points_in_plane_instance(68, 17, 2, 13, 2),
    random_points_in_plane_instance(68, 34, 3, 13),
    random_points_in_plane_instance(70, 17, 2, 14),
    random_points_in_plane_instance(70, 35, 3, 14),
    random_points_in_plane_instance(72, 18, 2, 14),
    random_points_in_plane_instance(72, 36, 3, 14, 2),
    random_points_in_plane_instance(74, 18, 2, 14),
    random_points_in_plane_instance(74, 37, 3, 14),
    random_points_in_plane_instance(76, 19, 2, 15, 2),
    random_points_in_plane_instance(76, 38, 3, 15),
    random_points_in_plane_instance(78, 19, 2, 15),
    random_points_in_plane_instance(78, 39, 3, 15),
    random_points_in_plane_instance(80, 20, 2, 16),
    random_points_in_plane_instance(80, 40, 3, 16)
]


def _dir_naming_convention(instance_name: str):
    return "../resources/instance_{}/".format(instance_name)


def generate_cases():
    for instance in INSTANCES:
        write_to_files(instance)
    # for nodes in range(10, 31):
    #     cust1 = int(nodes / 4)
    #     cust2 = int(nodes / 2)
    #     cust3 = int(3 * nodes / 4)
    #     k = int(nodes / 5)
    #     print(f"random_points_in_plane_instance({nodes}, {cust1}, {2}, {k}),")
    #     print(f"random_points_in_plane_instance({nodes}, {cust2}, {3}, {k}),")
    #     print(f"random_points_in_plane_instance({nodes}, {cust3}, {5}, {k}),")
    # for nodes in range(32, 81, 2):
    #     cust1 = int(nodes / 4)
    #     cust2 = int(nodes / 2)
    #     cust3 = int(3 * nodes / 4)
    #     k = int(nodes / 5)
    #     print(f"random_points_in_plane_instance({nodes}, {cust1}, {2}, {k}),")
    #     print(f"random_points_in_plane_instance({nodes}, {cust2}, {3}, {k}),")
    #     print(f"random_points_in_plane_instance({nodes}, {cust3}, {5}, {k}),")


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
