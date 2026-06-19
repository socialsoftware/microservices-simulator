from sb3_contrib import MaskablePPO
from sb3_contrib.common.wrappers import ActionMasker
from sb3_contrib.common.maskable.callbacks import MaskableEvalCallback
from stable_baselines3.common.callbacks import CheckpointCallback
from stable_baselines3.common.monitor import Monitor
from src.agents.rl.environments.environment import MicroserviceOptimizerEnv
from src.agents.rl.rewards.reward_strategies import RewardStrategyFactory
from src.agents.rl.observation_spaces.observation_strategies import ObservationStrategyFactory
from src.agents.simulation_runner import SimRunner
import os
import yaml
import logging


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
    hyperparameters = config["ppo_hyperparameters"]
    train_cfg = config["training"]
    paths = config["paths"]

    sim_runner = SimRunner(trace_manager)
    reward_strat = RewardStrategyFactory.create(
        environment["reward_type"], alpha=environment["alpha"], beta=environment["beta"])
    observation_strat = ObservationStrategyFactory.create(
        environment["observation_type"], environment["microservices_num"], environment["nodes_num"])

    env = MicroserviceOptimizerEnv(
        sim_runner,
        environment["workloads"],
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

    os.makedirs(paths["models_dir"], exist_ok=True)
    os.makedirs(paths["checkpoints_dir"], exist_ok=True)

    eval_callback = MaskableEvalCallback(
        env,
        best_model_save_path=paths["models_dir"],
        log_path=paths["models_dir"],
        eval_freq=train_cfg["eval_freq"],
        deterministic=True,
        render=False
    )

    checkpoint_callback = CheckpointCallback(
        save_freq=train_cfg["checkpoint_freq"],
        save_path=paths["checkpoints_dir"],
        name_prefix='ppo_model'
    )

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

    logging.info(
        f"Starting Training for {train_cfg['total_timesteps']} timesteps...")
    model.learn(total_timesteps=train_cfg["total_timesteps"], callback=[
                eval_callback, checkpoint_callback])

    logging.info("Saving Final Model...")
    model.save(os.path.join(paths["models_dir"], "final_model.zip"))

    logging.info("Training Complete!")
