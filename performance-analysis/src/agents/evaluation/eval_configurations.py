import os
import json
import logging
from torch.utils.tensorboard import SummaryWriter
from src.simulator_tools.h2_utils import H2DBManager
from src.agents.utils.simulation_runner import SimRunner, WorkloadConfig
from src.agents.utils.tensorboard_metrics import aggregate_metrics
import yaml


def _load_config():
    config_path = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "rl", "ppo_config.yaml"
    )
    with open(config_path, "r") as f:
        return yaml.safe_load(f)


def start_baseline_eval(trace_manager, workload_path: str, config_path: str, worker_id: int = 1):
    """
    Evaluates a given baseline JSON configuration against a specific workload for an entire episode.
    It logs the metrics continuously to TensorBoard to create an interval of values.
    """

    # Properly configure environment to connect to the docker containers
    os.environ["GATEWAY_URL"] = f"http://localhost:{8080 + worker_id}"
    os.environ["H2_PORT"] = str(1521 + worker_id)

    from src.server import start_grpc_server
    from src.trace_collection.trace_collector import TraceManager

    trace_manager = TraceManager()
    grpc_srv = start_grpc_server(port=4319 + worker_id, tm=trace_manager)

    logging.info(
        f"Setting up database state for baseline evaluation...")
    H2DBManager.setup_db_state()

    sim_runner = SimRunner(trace_manager)

    try:
        with open(config_path, "r") as f:
            optimal_config = json.load(f)
        logging.info(f"Loaded configuration from {config_path}")
    except Exception as e:
        logging.error(f"Failed to load baseline config: {e}")
        grpc_srv.stop(0)
        return

    rl_config = _load_config()
    workload_cfg = rl_config["workloads"]
    train_cfg = rl_config["training"]

    users = workload_cfg["users"][1]  # Worst case
    iterations = workload_cfg["iterations"][1]  # Worst case
    read_w = workload_cfg["weights_ratio"][0]
    write_w = workload_cfg["weights_ratio"][0]
    wait_time = workload_cfg["wait_time"][0]
    run_time = workload_cfg["run-time"]
    steps = train_cfg.get("steps_per_episode", 15)

    wl_config = WorkloadConfig(
        file=workload_path,
        users=users,
        spawn_rate=users if users <= 100 else 100,
        iterations=iterations,
        runtime_seconds=run_time,
        read_weight=read_w,
        write_weight=write_w,
        wait_time=wait_time
    )

    project_root = os.path.dirname(os.path.dirname(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
    tb_log_dir = os.path.join(
        project_root, "tensorboard_logs", "baseline_eval")
    os.makedirs(tb_log_dir, exist_ok=True)
    writer = SummaryWriter(log_dir=tb_log_dir)

    logging.info(
        f"Running baseline workload ({wl_config.file}) for {steps} steps (1 full episode)...")

    try:
        for step in range(1, steps + 1):
            logging.info(f"Executing Step {step}/{steps}...")

            metrics = sim_runner.evaluate_configuration(
                optimal_config, wl_config)

            aggregated = aggregate_metrics([metrics])
            for key, val in aggregated.items():
                writer.add_scalar(key, val, step)

    finally:
        writer.close()
        grpc_srv.stop(0)

    logging.info(f"Baseline finished! Logs saved to {tb_log_dir}.")
