import json
import subprocess
import logging
import os
import sys
from src.simulator_tools.simulator_utils import *
from src.simulator_tools.h2_utils import *
from dataclasses import dataclass


@dataclass
class WorkloadConfig:
    """Represents the workload profiles executed by the agent in each episode."""
    file: str
    users: int
    spawn_rate: int
    iterations: int
    runtime_seconds: int
    read_weight: float = 1.0
    write_weight: float = 1.0
    wait_time: float = 0.5


class SimRunner:
    """
    Class responsible for bridging the agent and the simulator.
    Abstracts the type of agent and algorithm implemented from the workloads and data collection.
    """

    def __init__(self, trace_collector):
        self.trace_collector = trace_collector
        self.base_config = self._read_base_config()
        self.current_config = self.base_config

    def _read_base_config(self) -> dict:
        """Loads and returns base configuration from initial_config/config.json."""

        config_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "initial_config",
            "config.json"
        )

        try:
            with open(config_path, 'r') as f:
                logging.info(
                    f"Loaded base configuration from {config_path}")
                return json.load(f)
        except Exception as e:
            logging.error(
                f"Failed to load base config from {config_path}: {e}")
            return {}

    def _run_workload(self, workload: WorkloadConfig):
        """Executes the workload configuration using a Locust subprocess."""

        workloads_dir = os.path.join(os.path.dirname(
            os.path.dirname(os.path.abspath(__file__))), "workloads")

        cmd = [
            sys.executable, "-m", "locust",
            "-f", workload.file,
            "--headless",
            "--only-summary",
            "-u", str(workload.users),
            "-r", str(workload.spawn_rate),
            "--read-weight", str(workload.read_weight),
            "--write-weight", str(workload.write_weight),
            "--wait-time", str(workload.wait_time)
        ]

        # As explain in the yaml configuration, run-time is the default behavior
        # Set it to 0 in order to use iterations
        if workload.runtime_seconds > 0:
            cmd.extend(["-t", str(workload.runtime_seconds)])
        else:
            cmd.extend(["--iterations", str(workload.iterations)])

        try:
            # We use subprocess.run to wait for the workload to finish
            # Suppress stdout and stderr so it doesn't pollute the CLI
            logging.info(
                f"cmd: {cmd}")
            subprocess.run(
                cmd,
                check=False,
                cwd=workloads_dir,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                timeout=100
            )
            logging.info(
                f"Workload {workload.file} completed.")
        except subprocess.TimeoutExpired as e:
            logging.error(
                f"Workload {workload.file} timed out.")
        except subprocess.CalledProcessError as e:
            logging.error(
                f"Workload {workload.file} execution failed: {e}")

    def evaluate_configuration(self, config: dict, workload: WorkloadConfig) -> dict:
        """Injects a new configuration, runs the workload and returns the resulting metrics."""

        # Reset metrics and DB so every config starts from the same point
        self.trace_collector.reset()
        H2DBManager.reset_db_state()

        SimInterface.inject_configuration(config)
        self.current_config = config

        # TODO: run warmup?
        self._run_workload(workload)

        self.trace_collector.wait_for_data()
        return self.trace_collector.get_metrics()
