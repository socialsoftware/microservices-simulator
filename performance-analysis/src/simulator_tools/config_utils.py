import copy
import random


class ConfigTool:
    """
    Centralized utility to interact with the simulator's configuration JSON.
    """

    @staticmethod
    def get_nodes(config: dict) -> list[dict]:
        """Returns the list of nodes from the placement config."""
        return config.get("Placement", {}).get("nodes", [])

    @staticmethod
    def get_microservices_list(config: dict) -> list[str]:
        """Returns a list of all microservice names from the config."""
        ms_caps = config.get("Capacities", {}).get("microservices", [])
        return sorted([ms["name"] for ms in ms_caps])

    @staticmethod
    def get_node_limit(node_subconfig: dict) -> int:
        """Returns the capacity limit of a node. Defaults to 100."""
        return node_subconfig.get("capacity", 100)

    @staticmethod
    def get_ms_capacity(config: dict, microservice_name: str) -> int:
        """Returns the allocated capacity of a specific microservice."""
        ms_caps = config.get("Capacities", {}).get("microservices", [])
        for ms in ms_caps:
            if ms["name"] == microservice_name:
                return ms["capacity"]
        return 1

    @staticmethod
    def get_node_capacities(config: dict) -> dict:
        """
        Returns a dictionary mapping node_index to its capacity limits and usage.
        Format: {0: {'limit': 100, 'used': 30}, 1: ...}
        """
        node_caps = {}
        ms_caps_dict = ConfigTool.get_all_ms_capacities(config)
        nodes = ConfigTool.get_nodes(config)

        for i, node in enumerate(nodes):
            node_caps[i] = {
                "limit": ConfigTool.get_node_limit(node),
                "used": sum(ms_caps_dict.get(ms, 0) for ms in node.get("microservices", []))
            }
        return node_caps

    @staticmethod
    def get_all_ms_capacities(config: dict) -> dict[str, int]:
        """Returns a dictionary mapping microservice name to its capacity."""
        ms_caps = config.get("Capacities", {}).get("microservices", [])
        return {ms["name"]: ms["capacity"] for ms in ms_caps}

    @staticmethod
    def get_ms_placement_map(config: dict) -> dict[str, int]:
        """
        Returns a dictionary mapping microservice name to its node index.
        Format: {"user": 0, "quiz": 1, ...}
        """
        placement_map = {}
        nodes = ConfigTool.get_nodes(config)
        for i, node in enumerate(nodes):
            for ms in node.get("microservices", []):
                placement_map[ms] = i
        return placement_map

    @staticmethod
    def randomize_config(base_config: dict, microservices: list[str], num_nodes: int) -> dict:
        """
        Generates a randomized configuration (placement and capacities).
        """

        config = copy.deepcopy(base_config)
        nodes = ConfigTool.get_nodes(config)

        # Randomize placement
        for node in nodes:
            node["microservices"] = []

        for microservice in microservices:
            node_idx = random.randint(0, num_nodes - 1)
            nodes[node_idx]["microservices"].append(microservice)

        # Randomize capacities
        microservice_caps = config.get(
            "Capacities", {}).get("microservices", [])

        for node in nodes:
            services_in_node = node["microservices"]
            if not services_in_node:
                continue

            node_limit = ConfigTool.get_node_limit(node)

            if len(services_in_node) > node_limit:
                # There cannot be more services that the node limit
                raise ValueError(
                    f"Node {node.get('name')} limit ({node_limit}) exceeded by base requirement ({len(services_in_node)} services).")

            # Guarantee at least 1 capacity per service
            allocations = {ms: 1 for ms in services_in_node}

            # Target 70% to 80% utilization to leave headroom for the agent
            target_utilization = random.uniform(0.7, 0.8)
            node_target_limit = int(node_limit * target_utilization)
            remaining = node_target_limit - len(services_in_node)

            remaining = max(0, remaining)

            # Randomly distribute all remaining capacity between services
            for _ in range(remaining):
                allocations[random.choice(services_in_node)] += 1

            for microservice in microservice_caps:
                if microservice["name"] in allocations:
                    microservice["capacity"] = allocations[microservice["name"]]

        return config

    @staticmethod
    def migrate_microservice(config: dict, ms_name: str, target_node_id: int) -> bool:
        """
        Attempts to move a microservice to a target node.
        Returns True if successful, False if the target node lacks capacity.
        """

        nodes = ConfigTool.get_nodes(config)
        microservice_cap = ConfigTool.get_ms_capacity(config, ms_name)

        current_node_id = None
        for i, node in enumerate(nodes):
            if ms_name in node["microservices"]:
                current_node_id = i
                break

        if current_node_id is None:
            return False

        if current_node_id == target_node_id:
            return True  # Already there

        node_caps = ConfigTool.get_node_capacities(config)
        target_node_cap_available = node_caps[target_node_id]["limit"] - \
            node_caps[target_node_id]["used"]

        if microservice_cap <= target_node_cap_available:
            nodes[current_node_id]["microservices"].remove(ms_name)
            nodes[target_node_id]["microservices"].append(ms_name)
            return True

        return False

    @staticmethod
    def scale_up_capacity(config: dict, ms_name: str, amount: int) -> bool:
        """
        Attempts to increase the capacity of a microservice by 'amount'.
        Returns True if successful, False if the node lacks capacity.
        """

        nodes = ConfigTool.get_nodes(config)
        current_node_id = None
        for i, node in enumerate(nodes):
            if ms_name in node["microservices"]:
                current_node_id = i
                break

        if current_node_id is None:
            return False

        node_capacities = ConfigTool.get_node_capacities(config)
        free_capacity = node_capacities[current_node_id]["limit"] - \
            node_capacities[current_node_id]["used"]

        if amount <= free_capacity:
            microservices_caps = config.get(
                "Capacities", {}).get("microservices", [])
            ms_dict = next(
                (m for m in microservices_caps if m["name"] == ms_name), None)
            if ms_dict:
                ms_dict["capacity"] += amount
                return True

        return False

    @staticmethod
    def scale_down_capacity(config: dict, ms_name: str, amount: int) -> bool:
        """
        Attempts to decrease the capacity of a microservice by 'amount'.
        Returns True if successful, False if it would drop to or below 0.
        """

        microservices = config.get("Capacities", {}).get("microservices", [])
        ms_dict = next(
            (m for m in microservices if m["name"] == ms_name), None)

        if ms_dict and ms_dict["capacity"] > amount:
            ms_dict["capacity"] -= amount
            return True

        return False
