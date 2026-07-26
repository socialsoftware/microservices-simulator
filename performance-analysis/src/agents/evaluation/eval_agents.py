from src.agents.rl.eval_ppo import evaluate_model
import logging


def start_evaluation(
    trace_collector,
    agent: str,
    model_path: str
):
    """
    Evalutes an agent against the current configuration.
    """

    if agent == "ppo":
        logging.info(f"Evaluating PPO trained model at {model_path}")
        evaluate_model(trace_collector, model_path)
    else:
        logging.warning("Invalid Arguments")
