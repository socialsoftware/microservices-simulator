from sb3_contrib.common.wrappers import ActionMasker
from stable_baselines3.common.monitor import Monitor
from contextlib import contextmanager
from src.trace_collection.trace_collector import TraceManager
from src.simulator_tools.h2_utils import H2DBManager
from src.agents.rl.environments.environment import MicroserviceOptimizerEnv
from src.agents.utils.simulation_runner import SimRunner
from src.agents.rl.rewards.reward_strategies import RewardStrategyFactory
from src.agents.rl.observation_spaces.observation_strategies import ObservationStrategyFactory
import yaml
import os


def load_ppo_config() -> dict:
    """
    Loads the ppo_config.yaml file from the rl directory.
    """

    config_path = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "rl",
        "ppo_config.yaml"
    )
    with open(config_path, "r") as f:
        return yaml.safe_load(f)


@contextmanager
def start_trace_server(worker_id: int):
    """
    Context manager to safely start and stop the TraceManager and gRPC server.
    Ensures ports are freed even on exceptions.
    """

    from src.server import start_grpc_server

    trace_manager = TraceManager()
    grpc_srv = start_grpc_server(port=4319 + worker_id, tm=trace_manager)
    try:
        yield trace_manager
    finally:
        grpc_srv.stop(0)


def create_rl_env(trace_manager: TraceManager, worker_id: int = 1, is_training: bool = False):
    """
    Creates an instace of MicroserviceOptimizerEnv with all strategies, wrappers, and env vars.
    """

    os.environ["GATEWAY_URL"] = f"http://localhost:{8080 + worker_id}"
    os.environ["H2_PORT"] = str(1521 + worker_id)

    from src.simulator_tools.simulator_utils import SimInterface
    import logging
    logging.info(f"Waiting for simulator to boot on {os.environ['GATEWAY_URL']}...")
    if not SimInterface.wait_for_simulator():
        logging.error("Timed out waiting for simulator to boot!")

    if is_training:
        H2DBManager.setup_db_state()

    config = load_ppo_config()
    environment = config["environment"]
    workload_cfg = config["workloads"]
    train_cfg = config["training"]

    sim_runner = SimRunner(trace_manager)
    reward_strat = RewardStrategyFactory.create(
        environment["reward_type"], alpha=environment["alpha"], beta=environment["beta"]
    )
    observation_strat = ObservationStrategyFactory.create(
        environment["observation_type"],
        environment["microservices_num"],
        environment["nodes_num"],
        run_time=workload_cfg["run-time"]
    )

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

    if is_training:
        def mask_fn(e):
            return e.valid_action_mask()

        env = ActionMasker(env, mask_fn)
        env = Monitor(env)

    return env
