import os
import json
from sb3_contrib import MaskablePPO
from src.agents.rl.environments.environment import MicroserviceOptimizerEnv
from src.agents.utils.simulation_runner import SimRunner
from src.agents.rl.rewards.reward_strategies import RewardStrategyFactory
from src.agents.rl.observation_spaces.observation_strategies import ObservationStrategyFactory
import yaml


def _load_config():
    config_path = os.path.join(os.path.dirname(
        os.path.abspath(__file__)), "ppo_config.yaml")
    with open(config_path, "r") as f:
        return yaml.safe_load(f)


def evaluate_model(trace_manager, model_path: str, worker_id: int = 1):
    # Start the environment
    os.environ["GATEWAY_URL"] = f"http://localhost:{8080 + worker_id}"
    os.environ["H2_PORT"] = str(1521 + worker_id)

    from src.server import start_grpc_server
    from src.trace_collection.trace_collector import TraceManager

    trace_manager = TraceManager()
    grpc_srv = start_grpc_server(port=4319 + worker_id, tm=trace_manager)

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

    model = MaskablePPO.load(model_path, env=env)

    obs, _ = env.reset(options={"deterministic": True})

    step = 0
    total_reward = 0
    while True:
        step += 1
        print(f"\n--- EVAL STEP {step} ---")

        action_masks = env.valid_action_mask()

        # deterministic=True means we always pick the absolute best action
        action, _ = model.predict(
            obs, action_masks=action_masks, deterministic=True)
        action_val = int(action)

        obs, reward, terminated, truncated, _ = env.step(action_val)
        total_reward += reward
        print(
            f"Reward: {reward:.4f} | Total Reward so far: {total_reward:.4f}")

        if terminated or truncated:
            print(
                f"\n=== EVALUATION FINISHED With Total Episode Reward: {total_reward:.4f} ===\n")
            print("Final Optimized Environment Configuration:")
            print(json.dumps(env.sim_runner.current_config, indent=2))
            break

    grpc_srv.stop(0)
