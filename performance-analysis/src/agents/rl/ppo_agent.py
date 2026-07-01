from sb3_contrib import MaskablePPO
from sb3_contrib.common.wrappers import ActionMasker
from sb3_contrib.common.maskable.callbacks import MaskableEvalCallback
from stable_baselines3.common.callbacks import CheckpointCallback, BaseCallback
from stable_baselines3.common.monitor import Monitor
from src.agents.rl.environments.environment import MicroserviceOptimizerEnv
from src.agents.rl.rewards.reward_strategies import RewardStrategyFactory
from src.agents.rl.observation_spaces.observation_strategies import ObservationStrategyFactory
from src.agents.simulation_runner import SimRunner
import os
import yaml
import logging


class CustomTensorboardCallback(BaseCallback):
    """
    Custom callback for plotting additional metrics in TensorBoard.
    Extracts the physical queue and delay times from the environment's last metrics.
    """

    def __init__(self, verbose=0):
        super().__init__(verbose)

    def _on_step(self) -> bool:
        last_metrics = self.training_env.get_attr('last_metrics')[0]

        if last_metrics:
            ms_metrics = last_metrics.get("microservices", {})

            total_queue = sum(m.get("queue_time", 0.0)
                              for m in ms_metrics.values())
            total_delay = sum(m.get("delay_time", 0.0)
                              for m in ms_metrics.values())
            total_invs = sum(m.get("invocations", 0)
                             for m in ms_metrics.values())

            if total_invs > 0:
                avg_queue = total_queue / total_invs
                avg_delay = total_delay / total_invs

                # These will appear in TensorBoard under "custom_metrics"
                self.logger.record(
                    "custom_metrics/avg_queue_time_ms", avg_queue)
                self.logger.record(
                    "custom_metrics/avg_delay_time_ms", avg_delay)
                self.logger.record(
                    "custom_metrics/total_invocations", total_invs)

        return True


def _load_config():
    config_path = os.path.join(os.path.dirname(
        os.path.abspath(__file__)), "ppo_config.yaml")
    with open(config_path, "r") as f:
        return yaml.safe_load(f)


def run_ppo(
    trace_manager,
):
    """
    Starts PPO training loop.
    Loads all parameters from ppo_config.yaml.
    """

    config = _load_config()
    environment = config["environment"]
    workload_cfg = config["workloads"]
    hyperparameters = config["ppo_hyperparameters"]
    train_cfg = config["training"]
    paths = config["paths"]

    # ==========================================
    # Setup Training Environment
    # ==========================================

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

    def mask_fn(env):
        return env.valid_action_mask()

    env = ActionMasker(env, mask_fn)
    env = Monitor(env)

    # ==========================================
    # Setup Evaluation Environment
    # ==========================================

    eval_sim_runner = SimRunner(trace_manager)

    eval_env = MicroserviceOptimizerEnv(
        eval_sim_runner,
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

    eval_env = ActionMasker(eval_env, mask_fn)
    eval_env = Monitor(eval_env)

    os.makedirs(paths["models_dir"], exist_ok=True)
    os.makedirs(paths["checkpoints_dir"], exist_ok=True)

    eval_callback = MaskableEvalCallback(
        eval_env,
        best_model_save_path=paths["models_dir"],
        log_path=paths["models_dir"],
        eval_freq=train_cfg["eval_freq"],
        # n_eval_episodes=1,  # (for testing)
        deterministic=True,
        render=False
    )

    checkpoint_callback = CheckpointCallback(
        save_freq=train_cfg["checkpoint_freq"],
        save_path=paths["checkpoints_dir"],
        name_prefix='ppo_model'
    )

    # ==========================================
    # Setup Agent Model
    # ==========================================

    model = MaskablePPO(
        "MultiInputPolicy",
        env,
        learning_rate=hyperparameters["learning_rate"],
        gamma=hyperparameters["gamma"],
        ent_coef=hyperparameters["ent_coef"],
        n_steps=hyperparameters["n_steps"],
        batch_size=hyperparameters["batch_size"],
        n_epochs=hyperparameters["n_epochs"],
        clip_range=hyperparameters["clip_range"],
        verbose=1,
        tensorboard_log=paths["tensorboard_log"]
    )

    custom_tb_callback = CustomTensorboardCallback()

    logging.info(
        f"Starting Training for {train_cfg['total_timesteps']} timesteps...")
    model.learn(total_timesteps=train_cfg["total_timesteps"], callback=[
                eval_callback, checkpoint_callback, custom_tb_callback])

    logging.info("Saving Final Model...")
    model.save(os.path.join(paths["models_dir"], "final_model.zip"))

    logging.info("Training Complete!")
