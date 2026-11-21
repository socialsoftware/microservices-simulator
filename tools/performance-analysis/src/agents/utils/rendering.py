import logging


def print_complex_observation(obs: dict):
    """
    Prints the normalized observation space dictionaries in humam-readable text using a single logging call.
    """

    output = [
        "Observation:",
        f" Capacities: {obs.get('capacities')}",
        f" Placements: {obs.get('placement')}"
    ]
    if obs.get("node_free_caps") is not None:
        output.append(f" Nodes Unused Capacity: {obs['node_free_caps']}")
    if obs.get("ms_delay") is not None:
        output.append(f" Delay Time: {obs['ms_delay']}")
    if obs.get("ms_queue") is not None:
        output.append(f" Queue Time: {obs['ms_queue']}")
    if obs.get("ms_load") is not None:
        output.append(f" MS Load: {obs['ms_load']}")

    logging.info("\n".join(output))


def print_action(action_idx: int, action_tuple: tuple, microservices: list):
    """
    Prints an action-tuple in humam-readable text using logging.
    """

    action_type, ms_idx, target = action_tuple
    ms_name = microservices[ms_idx] if ms_idx is not None else "None"

    action_desc = "UNKNOWN"

    if action_type == 0:
        action_desc = "STOP (No operation)"
    elif action_type == 1:
        action_desc = f"MIGRATE microservice '{ms_name}' to node {target}"
    elif action_type == 2:
        amount = 5 * target  # base AMOUNT=5 * multiplier
        action_desc = f"SCALE UP microservice '{ms_name}' by {amount} capacity"
    elif action_type == 3:
        amount = 5 * target
        action_desc = f"SCALE DOWN microservice '{ms_name}' by {amount} capacity"

    logging.info(
        f"Agent chose action {action_idx}: {action_tuple} -> {action_desc}")
