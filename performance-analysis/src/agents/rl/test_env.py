import random
import os
import yaml
from stable_baselines3.common.env_checker import check_env
from src.agents.rl.rewards.reward_strategies import RewardStrategyFactory
from src.agents.rl.observation_spaces.observation_strategies import ObservationStrategyFactory
from src.agents.rl.environments.environment import MicroserviceOptimizerEnv
from src.agents.simulation_runner import SimRunner


def _load_config():
    config_path = os.path.join(os.path.dirname(
        os.path.abspath(__file__)), "ppo_config.yaml")
    with open(config_path, "r") as f:
        return yaml.safe_load(f)


def _print_obs(obs: dict):
    print(
        f"Observation:\n Capacities: {obs['capacities']}\n Placements: {obs['placement']}")
    if obs["node_free_caps"] is not None:
        print(f" Nodes Unused Capacity: {obs["node_free_caps"]}")
    if obs["ms_delay"] is not None:
        print(f" Delay Time: {obs["ms_delay"]}")
    if obs["ms_queue"] is not None:
        print(f" Queue Time: {obs["ms_queue"]}")


def run_sanity_check(trace_manager):
    config = _load_config()
    environment = config["environment"]
    workload_cfg = config["workloads"]
    train_cfg = config["training"]

    sim_runner = SimRunner(trace_manager)
    reward_strat = RewardStrategyFactory.create(
        environment["reward_type"], alpha=environment["alpha"], beta=environment["beta"])
    observation_strat = ObservationStrategyFactory.create(
        environment["observation_type"], environment["microservices_num"], environment["nodes_num"], run_time=workload_cfg["run-time"])

    env = MicroserviceOptimizerEnv(
        sim_runner,
        workload_cfg["workloads"],
        tuple(workload_cfg["users"]),
        tuple(workload_cfg["iterations"]),
        tuple(workload_cfg["weights_ratio"]),
        tuple(workload_cfg["wait_time"]),
        workload_cfg["run-time"],
        reward_strat,
        observation_strat,
        environment["microservices_num"],
        environment["nodes_num"],
        train_cfg["steps_per_episode"]
    )

    print("\n=== 1. RUNNING SB3 API CHECK ===\n\n")
    # This checks for shape mismatches, NaN values, and ensures observations are legal
    check_env(env, warn=True)
    print("\nAPI Check Passed!\n")

    print("\n\n=== 2. RUNNING MANUAL STEP TEST ===\n\n")
    obs, _ = env.reset()

    wl = env.wl_config
    print(f"Generated Workload: File={wl.file}, Users={wl.users}, Iterations={wl.iterations}, "
          f"Run_Time: {wl.runtime_seconds}, Read_W={wl.read_weight:.2f}, Write_W={wl.write_weight:.2f}, Wait_T={wl.wait_time:.2f}\n")

    _print_obs(obs)

    for step in range(1, 4):
        print(f"\n--- STEP {step} ---")

        # Get only the valid actions using the mask
        valid_actions = [i for i, valid in enumerate(
            env.valid_action_mask()) if valid]

        # Pick a random valid action (ensure we don't pick NO_OP (0) so the episode doesn't end)
        action = random.choice([a for a in valid_actions if a != 0])
        action_tuple = env.action_mapping[action]

        action_type, ms_idx, target = action_tuple
        ms_name = env.microservices[ms_idx] if ms_idx is not None else "None"

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
        else:
            action_desc = "UNKNOWN"

        print(f"Agent chose action {action}: {action_tuple} -> {action_desc}")

        # Take the step
        obs, reward, terminated, truncated, _ = env.step(action)
        print(f"Reward: {reward:.4f}")
        _print_obs(obs)

        if terminated or truncated:
            print("Episode ended.")
            break
