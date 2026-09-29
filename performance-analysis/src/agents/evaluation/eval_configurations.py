import os
import json
import logging
from collections import defaultdict
import numpy as np
from torch.utils.tensorboard import SummaryWriter
from src.simulator_tools.h2_utils import H2DBManager
from src.simulator_tools.config_utils import ConfigTool
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


def _format_summary_table(
    config_path: str,
    workload_path: str,
    steps: int,
    tb_log_dir: str,
    global_queues: list,
    global_delays: list,
    global_invs: list,
    ms_data: dict
) -> str:
    lines = []
    header_sep = "=" * 94
    sub_sep = "-" * 94
    lines.append("\n" + header_sep)
    lines.append("                         BASELINE EVALUATION SUMMARY")
    lines.append(header_sep)
    lines.append(f"Configuration: {config_path}")
    lines.append(f"Workload:      {workload_path}")
    lines.append(f"Steps:         {steps}")
    lines.append(f"TensorBoard:   {tb_log_dir}")
    lines.append(sub_sep)

    g_mean_q = float(np.mean(global_queues)) if global_queues else 0.0
    g_med_q = float(np.median(global_queues)) if global_queues else 0.0
    g_mean_d = float(np.mean(global_delays)) if global_delays else 0.0
    g_med_d = float(np.median(global_delays)) if global_delays else 0.0
    g_mean_inv = float(np.mean(global_invs)) if global_invs else 0.0
    g_med_inv = float(np.median(global_invs)) if global_invs else 0.0
    g_tot_inv = int(np.sum(global_invs)) if global_invs else 0

    lines.append("GLOBAL METRICS (Across Steps):")
    lines.append(f"  - Avg Queue Time (ms):  Mean: {g_mean_q:9.3f} ms | Median: {g_med_q:9.3f} ms")
    lines.append(f"  - Avg Delay Time (ms):  Mean: {g_mean_d:9.3f} ms | Median: {g_med_d:9.3f} ms")
    lines.append(f"  - Invocations per Step: Mean: {g_mean_inv:9.2f}    | Median: {g_med_inv:9.2f}    | Total: {g_tot_inv}")
    lines.append(sub_sep)
    lines.append("PER-MICROSERVICE METRICS (Across Steps):")
    col1 = "Microservice"
    col2 = "Queue Time (ms)"
    col3 = "Delay Time (ms)"
    col4 = "Invocations / Step"
    lines.append(f"{col1:<20} | {col2:<21} | {col3:<21} | {col4:<21}")
    m1 = "Mean"
    m2 = "Median"
    blank = ""
    lines.append(f"{blank:<20} | {m1:<10} {m2:<10} | {m1:<10} {m2:<10} | {m1:<10} {m2:<10}")
    lines.append(sub_sep)

    for ms_name in sorted(ms_data.keys()):
        m = ms_data[ms_name]
        q_mean = float(np.mean(m["queues"])) if m["queues"] else 0.0
        q_med = float(np.median(m["queues"])) if m["queues"] else 0.0
        d_mean = float(np.mean(m["delays"])) if m["delays"] else 0.0
        d_med = float(np.median(m["delays"])) if m["delays"] else 0.0
        inv_mean = float(np.mean(m["invs"])) if m["invs"] else 0.0
        inv_med = float(np.median(m["invs"])) if m["invs"] else 0.0

        lines.append(
            f"{ms_name:<20} | {q_mean:9.3f}  {q_med:9.3f}  | {d_mean:9.3f}  {d_med:9.3f}  | {inv_mean:9.2f}  {inv_med:9.2f}"
        )

    lines.append(header_sep + "\n")
    return "\n".join(lines)


def start_baseline_eval(trace_manager, workload_path: str, config_path: str, worker_id: int = 1):
    """
    Evaluates a given baseline JSON configuration against a specific workload for an entire episode.
    It logs the metrics continuously to TensorBoard to create an interval of values.
    """

    config_path = ConfigTool.resolve_config_path(config_path)
    workload_path = ConfigTool.resolve_workload_path(workload_path)

    # Properly configure environment to connect to the docker containers
    os.environ["GATEWAY_URL"] = f"http://localhost:{8080 + worker_id}"
    os.environ["H2_PORT"] = str(1521 + worker_id)

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

    project_root = ConfigTool.get_project_root()
    base_dir = os.path.join(
        project_root, "tensorboard_logs", "baseline_eval")
    counter = 1
    tb_log_dir = f"{base_dir}_{counter}"
    while os.path.exists(tb_log_dir):
        counter += 1
        tb_log_dir = f"{base_dir}_{counter}"

    os.makedirs(tb_log_dir, exist_ok=True)
    writer = SummaryWriter(log_dir=tb_log_dir)

    logging.info(
        f"Running baseline workload ({wl_config.file}) for {steps} steps (1 full episode)...")

    known_ms = set(ConfigTool.get_microservices_list(optimal_config))
    step_global_queues = []
    step_global_delays = []
    step_global_invs = []
    step_ms_data = defaultdict(lambda: {"queues": [], "delays": [], "invs": []})

    try:
        for step in range(1, steps + 1):
            logging.info(f"Executing Step {step}/{steps}...")

            metrics = sim_runner.evaluate_configuration(
                optimal_config, wl_config)

            aggregated = aggregate_metrics([metrics])
            for key, val in aggregated.items():
                writer.add_scalar(key, val, step)

            if "metrics_global/avg_queue_time_ms" in aggregated:
                step_global_queues.append(aggregated["metrics_global/avg_queue_time_ms"])
            if "metrics_global/avg_delay_time_ms" in aggregated:
                step_global_delays.append(aggregated["metrics_global/avg_delay_time_ms"])
            if "metrics_global/total_invocations" in aggregated:
                step_global_invs.append(aggregated["metrics_global/total_invocations"])

            ms_metrics = metrics.get("microservices", {})
            all_step_ms = known_ms.union(ms_metrics.keys())
            for ms_name in all_step_ms:
                ms_info = ms_metrics.get(ms_name, {})
                invs = ms_info.get("invocations", 0)
                q = ms_info.get("queue_time", 0.0)
                d = ms_info.get("delay_time", 0.0)
                step_ms_data[ms_name]["invs"].append(invs)
                if invs > 0:
                    step_ms_data[ms_name]["queues"].append(q / invs)
                    step_ms_data[ms_name]["delays"].append(d / invs)

    finally:
        writer.close()

    logging.info(f"Baseline finished! Logs saved to {tb_log_dir}.")

    summary_text = _format_summary_table(
        config_path,
        workload_path,
        steps,
        tb_log_dir,
        step_global_queues,
        step_global_delays,
        step_global_invs,
        step_ms_data
    )
    print(summary_text)

    try:
        with open(os.path.join(tb_log_dir, "summary.txt"), "w") as f:
            f.write(summary_text)
    except Exception as e:
        logging.warning(f"Could not save summary.txt to {tb_log_dir}: {e}")
