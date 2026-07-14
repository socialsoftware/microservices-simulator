from src.agents.rl.ppo_agent import run_ppo
from src.agents.rl.test_env import run_sanity_check
from src.simulator_tools.h2_utils import H2DBManager
from src.agents.simulation_runner import SimRunner, WorkloadConfig
import logging


def start_training(
    trace_collector,
    agent: str = "test"
):
    """
    Starts the agent training loop.
    """

    if agent == "ppo":
        logging.info("Initializing PPO agent")
        run_ppo(trace_collector)
    else:
        logging.info("Initializing TEST agent")
        run_sanity_check(trace_collector)
