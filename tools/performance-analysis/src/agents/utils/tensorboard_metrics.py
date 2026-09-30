import numpy as np


def aggregate_metrics(all_metrics: list):
    """
    Takes a list of metrics dictionaries, one from each environment.
    Computes normalized global and per-microservice stats. 

    Returns a flattened dictionary of keys and values ready for TensorBoard.
    """

    if not all_metrics:
        return {}

    global_queue_sum = 0.0
    global_delay_sum = 0.0
    global_inv_sum = 0

    ms_env_data = {}
    invocations_per_env = []

    for last_metrics in all_metrics:
        if not last_metrics:
            continue

        ms_metrics = last_metrics.get("microservices", {})
        environment_total_invs = 0

        for ms_name, metrics in ms_metrics.items():
            queue = metrics.get("queue_time", 0.0)
            delay = metrics.get("delay_time", 0.0)
            invs = metrics.get("invocations", 0)

            global_queue_sum += queue
            global_delay_sum += delay
            global_inv_sum += invs

            environment_total_invs += invs

            if ms_name not in ms_env_data:
                ms_env_data[ms_name] = {"queues": [], "delays": [], "invs": []}

            if invs > 0:
                ms_env_data[ms_name]["queues"].append(queue / invs)
                ms_env_data[ms_name]["delays"].append(delay / invs)

            ms_env_data[ms_name]["invs"].append(invs)

        invocations_per_env.append(environment_total_invs)

    results = {}

    if global_inv_sum > 0:
        results["metrics_global/avg_queue_time_ms"] = global_queue_sum / \
            global_inv_sum
        results["metrics_global/avg_delay_time_ms"] = global_delay_sum / \
            global_inv_sum

        mean_invocations = float(
            np.mean(invocations_per_env)) if invocations_per_env else 0.0
        results["metrics_global/total_invocations"] = mean_invocations

        for ms_name, metrics in ms_env_data.items():
            if metrics["queues"]:
                # Use median for latencies to filter out outliers across parallel workers
                results[f"metrics_ms_{ms_name}/avg_queue_time_ms"] = float(
                    np.median(metrics["queues"]))
                results[f"metrics_ms_{ms_name}/avg_delay_time_ms"] = float(
                    np.median(metrics["delays"]))
                results[f"metrics_ms_{ms_name}/total_invocations"] = float(
                    np.mean(metrics["invs"]))

    return results
