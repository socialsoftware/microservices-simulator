import random
from stable_baselines3.common.env_checker import check_env
from src.agents.utils.env_setup import start_trace_server, create_rl_env
from src.agents.utils.rendering import print_complex_observation, print_action


def run_sanity_check(worker_id: int = 1):
    with start_trace_server(worker_id) as trace_manager:
        env = create_rl_env(trace_manager, worker_id, is_training=False)

        print("\n=== 1. RUNNING SB3 API CHECK ===\n\n")
        # This checks for shape mismatches, NaN values, and ensures observations are legal
        check_env(env, warn=True)
        print("\nAPI Check Passed!\n")

        obs, _ = env.reset(options={"deterministic": True})

        wl = env.wl_config
        print(f"Generated Workload: File={wl.file}, Users={wl.users}, Iterations={wl.iterations}, "
              f"Run_Time: {wl.runtime_seconds}, Read_W={wl.read_weight:.2f}, Write_W={wl.write_weight:.2f}, Wait_T={wl.wait_time:.2f}\n")

        print_complex_observation(obs)

        for step in range(1, 4):
            print(f"\n--- STEP {step} ---")

            valid_actions = [i for i, valid in enumerate(
                env.valid_action_mask()) if valid]
            action = random.choice([a for a in valid_actions if a != 0])

            action_tuple = env.action_mapping[action]
            print_action(action, action_tuple, env.microservices)

            obs, reward, terminated, truncated, _ = env.step(action)
            print(f"Reward: {reward:.4f}")
            print_complex_observation(obs)

            if terminated or truncated:
                print("Episode ended.")
                break
