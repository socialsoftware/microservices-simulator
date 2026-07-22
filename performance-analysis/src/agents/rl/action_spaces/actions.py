from enum import IntEnum
import numpy as np
from src.simulator_tools.config_utils import ConfigTool


class Action(IntEnum):
    Stop = 0
    Migrate = 1
    ScaleUp = 2
    ScaleDown = 3


AMOUNT: int = 5  # Amount of capacity to scale


def get_action_mapping(num_services: int, num_nodes: int) -> list[tuple]:
    """
    Returns a list that translate integer indices back to all possible action tuples.
    """

    mapping = []

    # NO OP - 1 possibility
    mapping.append((Action.Stop.value, None, None))

    # Migration: (Action.Migrate, microsservice_id, target_node_id)
    # 3 nodes * 8 microservices = 24 possibilities
    for ms_id in range(num_services):
        for target_node_id in range(num_nodes):
            mapping.append((Action.Migrate.value, ms_id, target_node_id))

    # ScaleUp: (Action.ScaleUp, microsservice_id, multiplier)
    # 8 microservices * 5 orders of scale = 40 possibilities
    for ms_id in range(num_services):
        for multiplier in range(1, 6):
            mapping.append((Action.ScaleUp.value, ms_id, multiplier))

    # ScaleDown: (Action.ScaleDown, microsservice_id, multiplier)
    # 8 microservices * 5 orders of scale = 40 possibilities
    for ms_id in range(num_services):
        for multiplier in range(1, 6):
            mapping.append((Action.ScaleDown.value, ms_id, multiplier))

    return mapping


def get_valid_action_mask(action_mapping: list[tuple], config: dict,
                          microservices_list: list[str], amount: int = AMOUNT) -> np.ndarray:
    """
    Returns a boolean numpy array representing an action mask where True is legal and False is illegal.
    """

    num_of_actions = len(action_mapping)
    mask = np.zeros(num_of_actions, dtype=bool)
    mask[0] = True  # Stop action is always legal

    placement_map = ConfigTool.get_ms_placement_map(config)
    node_caps = ConfigTool.get_node_capacities(config)
    microservice_caps = ConfigTool.get_all_ms_capacities(config)

    for i in range(1, num_of_actions):
        action_tuple = action_mapping[i]
        action_type = action_tuple[0]

        if action_type == Action.Migrate:
            microservice_id = action_tuple[1]
            target_node_id = action_tuple[2]

            microservice_name = microservices_list[microservice_id]
            current_node_id = placement_map.get(microservice_name)
            microservice_cap = microservice_caps.get(microservice_name, 1)
            target_node_cap_available = node_caps[target_node_id]["limit"] - \
                node_caps[target_node_id]["used"]

            microservice_already_at_target_node = current_node_id == target_node_id
            microservice_cap_surpasses_target_node_limit = microservice_cap > target_node_cap_available

            if microservice_already_at_target_node or microservice_cap_surpasses_target_node_limit:
                continue

            mask[i] = True

        elif action_type == Action.ScaleUp:
            microservice_id = action_tuple[1]
            multiplier = action_tuple[2]

            total_amount = amount * multiplier
            microservice_name = microservices_list[microservice_id]

            current_node_id = placement_map.get(microservice_name)

            if current_node_id is not None:
                free_capacity = node_caps[current_node_id]["limit"] - \
                    node_caps[current_node_id]["used"]
                if total_amount <= free_capacity:
                    mask[i] = True

        elif action_type == Action.ScaleDown:
            microservice_id = action_tuple[1]
            multiplier = action_tuple[2]

            total_amount = amount * multiplier
            microservice_name = microservices_list[microservice_id]

            microservice_capacity = microservice_caps.get(microservice_name, 1)
            max_req = ConfigTool.get_ms_max_requirement(config, microservice_name)

            if microservice_capacity - total_amount >= max_req:
                mask[i] = True

    return mask
