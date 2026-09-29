import os
import json
import logging
from sb3_contrib import MaskablePPO
from src.agents.utils.env_setup import create_rl_env
from src.simulator_tools.h2_utils import H2DBManager
from src.simulator_tools.config_utils import ConfigTool


def evaluate_model(trace_manager, model_path: str, workload_path: str = None, config_path: str = None, worker_id: int = 1):
    resolved_config = ConfigTool.resolve_config_path(config_path) if config_path else None
    resolved_workload = ConfigTool.resolve_workload_path(workload_path) if workload_path else None

    env = create_rl_env(
        trace_manager,
        worker_id,
        is_training=False,
        custom_workload=resolved_workload,
        custom_config_path=resolved_config
    )

    H2DBManager.setup_db_state()

    logging.info(f"\n=== LOADING TRAINED MODEL: {model_path} ===\n")
    model = MaskablePPO.load(model_path, env=env)

    logging.info("\n=== STARTING EVALUATION EPISODE ===\n")
    obs, _ = env.reset(options={"deterministic": True})

    step = 0
    total_reward = 0
    while True:
        step += 1
        logging.info(f"\n--- EVAL STEP {step} ---")

        action_masks = env.valid_action_mask()
        # deterministic=True means we always pick the absolute best action
        action, _ = model.predict(
            obs, action_masks=action_masks, deterministic=True)
        action_val = int(action)

        obs, reward, terminated, truncated, _ = env.step(action_val)
        total_reward += reward
        logging.info(
            f"Total Reward so far: {total_reward:.4f}")

        if terminated or truncated:
            print(
                f"\n=== EVALUATION FINISHED With Total Episode Reward: {total_reward:.4f} ===\n")
            print("Final Optimized Environment Configuration:")
            print(json.dumps(
                env.sim_runner.current_config, indent=2))

            eval_results_dir = os.path.join(
                ConfigTool.get_project_root(), "models", "eval_results"
            )
            os.makedirs(eval_results_dir, exist_ok=True)
            output_path = os.path.join(eval_results_dir, "optimized_config.json")
            with open(output_path, "w") as f:
                json.dump(env.sim_runner.current_config, f, indent=2)
            print(f"\nSaved optimized configuration to: {output_path}")
            break
