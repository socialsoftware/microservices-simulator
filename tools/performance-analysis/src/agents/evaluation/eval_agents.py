from src.agents.rl.eval_ppo import evaluate_model
import logging


def start_evaluation(
    trace_collector,
    agent: str,
    model_path: str,
    workload_path: str = None,
    config_path: str = None
):
    """
    Evaluates an agent against the specified workload and configuration.
    """

    if agent == "ppo":
        logging.info(
            f"Evaluating PPO trained model at {model_path} with workload {workload_path} and config {config_path}")
        evaluate_model(trace_collector, model_path, workload_path, config_path)
    else:
        logging.warning("Invalid Arguments")
