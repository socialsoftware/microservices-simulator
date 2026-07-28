import json
import logging
from sb3_contrib import MaskablePPO
from src.agents.utils.env_setup import create_rl_env


def evaluate_model(trace_manager, model_path: str, worker_id: int = 1):
    env = create_rl_env(trace_manager, worker_id, is_training=False)

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
            break
