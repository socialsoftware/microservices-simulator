from src.agents.rl.ppo_agent import run_ppo
from src.simulator_tools.h2_utils import H2DBManager
from src.agents.simulation_runner import SimRunner, WorkloadConfig
import time

import logging
logging.basicConfig(level=logging.INFO)


def start_training(
    trace_collector,
    agent: str = "test"
):
    """
    Starts the agent training loop.
    """

    H2DBManager.setup_db_state()

    if agent == "ppo":
        logging.info("Initializing PPO agent")
        run_ppo(trace_collector)
    else:
        logging.info("Initializing DUMMY agent")
        _test_loop(trace_collector)


def _test_loop(trace_collector):
    """Small dummy loop for testing purposes."""

    evaluator = SimRunner(trace_collector)

    print("--- Starting Test Loop ---")

    total_start_time = time.time()

    config = evaluator.base_config

    num_steps = 1
    for step in range(num_steps):
        print(f"\n--- Step {step + 1}/{num_steps} ---")
        step_start_time = time.time()
        evaluator.evaluate_configuration(
            config, WorkloadConfig("tournament_period_wl.py", 50, 50, 500, 0))
        step_end_time = time.time()
        print(
            f"Step {step + 1} completed in {step_end_time - step_start_time:.2f} seconds.")

    total_end_time = time.time()
    print("--- Test Loop Finished ---")
    print(
        f"Total training time: {total_end_time - total_start_time:.2f} seconds.")
