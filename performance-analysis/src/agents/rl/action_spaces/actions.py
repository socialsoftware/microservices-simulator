from enum import IntEnum
import numpy as np
from src.simulator_tools.config_utils import ConfigTool


class Action(IntEnum):
    Stop = 0
    Migrate = 1
    Reallocate = 2


AMOUNT: int = 5  # Amount of capacity to reallocate


def get_action_mapping(num_services: int, num_nodes: int) -> list[tuple]:
    """
    Returns a list that translate integer indices back to all possible action tuples.
    """

    mapping = []

    # NO OP - 1 possibility
    mapping.append((Action.Stop.value, None, None, None))

    # Migration: (Action.Migrate, microsservice_id, target_node_id, None)
    # 3 nodes * 8 microservices = 24 possibilities
    for ms_id in range(num_services):
        for target_node_id in range(num_nodes):
            mapping.append((Action.Migrate.value, ms_id, target_node_id, None))

    # Reallocation: (Action.Reallocate, node_id, ms_to_increment_id, ms_to_decrement_id)
    # 3 nodes * 8 microservices * 8 microservices = 192 possiblities
    for node_id in range(num_nodes):
        for inc_ms_id in range(num_services):
            for dec_ms_id in range(num_services):
                if inc_ms_id != dec_ms_id:
                    mapping.append(
                        (Action.Reallocate.value, node_id, inc_ms_id, dec_ms_id))

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

        elif action_type == Action.Reallocate:
            node_id = action_tuple[1]
            incrementing_microservice_id = action_tuple[2]
            decrementing_microservice_id = action_tuple[3]

            inc_microservice_name = microservices_list[incrementing_microservice_id]
            dec_microservice_name = microservices_list[decrementing_microservice_id]

            inc_node_id = placement_map.get(inc_microservice_name)
            dec_node_id = placement_map.get(dec_microservice_name)
            dec_microservice_capacity = microservice_caps.get(
                dec_microservice_name, 1)

            microservices_not_in_the_selected_node = inc_node_id != node_id or dec_node_id != node_id
            decrementing_ms_does_not_have_enough_capacity = dec_microservice_capacity <= amount

            if microservices_not_in_the_selected_node or decrementing_ms_does_not_have_enough_capacity:
                continue

            mask[i] = True

    return mask
