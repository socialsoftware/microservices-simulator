from sb3_contrib import MaskablePPO
from stable_baselines3.common.callbacks import CheckpointCallback, BaseCallback
from stable_baselines3.common.vec_env import SubprocVecEnv
from src.agents.utils.tensorboard_metrics import aggregate_metrics
from src.agents.utils.env_setup import load_ppo_config
import os
import logging


class CustomTensorboardCallback(BaseCallback):
    """
    Custom callback for plotting additional metrics in TensorBoard.
    Normalizes metrics across parallel environment instances.
    """

    def __init__(self, verbose=0):
        super().__init__(verbose)

    def _on_step(self) -> bool:
        all_metrics = self.training_env.get_attr('last_metrics')
        aggregated = aggregate_metrics(all_metrics)

        for key, val in aggregated.items():
            self.logger.record(key, val)

        return True


def make_env(rank: int, log_level: int):
    def _init():
        import logging
        from src.agents.utils.env_setup import create_rl_env
        from src.trace_collection.trace_collector import TraceManager
        from src.server import start_grpc_server

        worker_id = rank + 1

        # Configure logging to match parent and include a prefix to distinguish workers
        logging.basicConfig(
            level=log_level,
            format=f'[Worker {worker_id}] %(levelname)s:%(name)s:%(message)s',
            force=True
        )

        trace_manager = TraceManager()
        grpc_srv = start_grpc_server(port=4319 + worker_id, tm=trace_manager)

        env = create_rl_env(trace_manager, worker_id, is_training=True)
        env.grpc_srv = grpc_srv  # Prevent GC!
        return env
    return _init


def run_ppo():
    """
    Starts PPO training loop.
    Loads all parameters from ppo_config.yaml.
    """

    config = load_ppo_config()
    hyperparameters = config["ppo_hyperparameters"]
    train_cfg = config["training"]
    paths = config["paths"]

    # Get the current logging level from the main process
    current_log_level = logging.getLogger().getEffectiveLevel()

    # ==========================================
    # Setup Training Environment
    # ==========================================
    num_envs = train_cfg.get("num_envs", 4)
    env = SubprocVecEnv([make_env(i, current_log_level)
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

    policy_kwargs = dict(
        net_arch=dict(
            pi=[256, 256],
            vf=[256, 256]
        )
    )

    reset_timesteps = True
    resume_path = train_cfg.get("resume_from_checkpoint", None)

    if resume_path and os.path.exists(resume_path):
        logging.info(f"Resuming training from checkpoint: {resume_path}")
        model = MaskablePPO.load(
            resume_path,
            env=env,
            tensorboard_log=paths["tensorboard_log"],
            custom_objects={"learning_rate": hyperparameters["learning_rate"]}
        )
        reset_timesteps = False
    else:
        logging.info("Initializing new PPO model...")
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
            policy_kwargs=policy_kwargs,
            verbose=1,
            tensorboard_log=paths["tensorboard_log"]
        )

    custom_tb_callback = CustomTensorboardCallback()

    logging.info(
        f"Starting Training for {train_cfg['total_timesteps']} timesteps with {num_envs} workers...")

    try:
        model.learn(total_timesteps=train_cfg["total_timesteps"], callback=[
            checkpoint_callback, custom_tb_callback], reset_num_timesteps=reset_timesteps)
    except KeyboardInterrupt:
        logging.info(
            "Training interrupted manually (Ctrl+C). Gracefully exiting and saving model...")

    logging.info("Saving Final Model...")
    model.save(os.path.join(paths["models_dir"], "final_model.zip"))

    logging.info("Training Complete!")
