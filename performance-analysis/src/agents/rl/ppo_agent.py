from sb3_contrib import MaskablePPO
from sb3_contrib.common.wrappers import ActionMasker
from sb3_contrib.common.maskable.callbacks import MaskableEvalCallback
from stable_baselines3.common.callbacks import CheckpointCallback, BaseCallback
from stable_baselines3.common.monitor import Monitor
from stable_baselines3.common.vec_env import SubprocVecEnv
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
        all_metrics = self.training_env.get_attr('last_metrics')

        global_queue = 0.0
        global_delay = 0.0
        global_invs = 0

        for last_metrics in all_metrics:
            if last_metrics:
                ms_metrics = last_metrics.get("microservices", {})
                global_queue += sum(m.get("queue_time", 0.0)
                                    for m in ms_metrics.values())
                global_delay += sum(m.get("delay_time", 0.0)
                                    for m in ms_metrics.values())
                global_invs += sum(m.get("invocations", 0)
                                   for m in ms_metrics.values())

        if global_invs > 0:
            avg_queue = global_queue / global_invs
            avg_delay = global_delay / global_invs

            # These will appear in TensorBoard under "custom_metrics"
            self.logger.record("custom_metrics/avg_queue_time_ms", avg_queue)
            self.logger.record("custom_metrics/avg_delay_time_ms", avg_delay)
            self.logger.record("custom_metrics/total_invocations", global_invs)

        return True


def make_env(rank: int, config: dict, log_level: int):
    def _init():
        import os
        import logging
        from src.trace_collection.trace_collector import TraceManager
        from src.server import start_grpc_server
        from src.simulator_tools.h2_utils import H2DBManager

        worker_id = rank + 1

        # Configure logging to match parent and include a prefix to distinguish workers
        logging.basicConfig(
            level=log_level,
            format=f'[Worker {worker_id}] %(levelname)s:%(name)s:%(message)s',
            force=True
        )

        os.environ["GATEWAY_URL"] = f"http://localhost:{8080 + worker_id}"
        os.environ["H2_PORT"] = str(1521 + worker_id)

        H2DBManager.setup_db_state()

        trace_manager = TraceManager()
        grpc_srv = start_grpc_server(port=4319 + worker_id, tm=trace_manager)

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

        def mask_fn(e):
            return e.valid_action_mask()

        env = ActionMasker(env, mask_fn)
        env = Monitor(env)
        env.grpc_srv = grpc_srv  # Prevent GC!
        return env
    return _init


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

    # Get the current logging level from the main process
    current_log_level = logging.getLogger().getEffectiveLevel()

    # ==========================================
    # Setup Training Environment
    # ==========================================
    num_envs = train_cfg.get("num_envs", 4)
    env = SubprocVecEnv([make_env(i, config, current_log_level)
                        for i in range(num_envs)])

    os.makedirs(paths["models_dir"], exist_ok=True)
    os.makedirs(paths["checkpoints_dir"], exist_ok=True)

    checkpoint_callback = CheckpointCallback(
        save_freq=max(1, train_cfg["checkpoint_freq"] // num_envs),
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
        f"Starting Training for {train_cfg['total_timesteps']} timesteps with {num_envs} workers...")
    model.learn(total_timesteps=train_cfg["total_timesteps"], callback=[
                checkpoint_callback, custom_tb_callback])

    logging.info("Saving Final Model...")
    model.save(os.path.join(paths["models_dir"], "final_model.zip"))

    logging.info("Training Complete!")
