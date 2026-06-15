import json
import subprocess
import logging
import os
import sys
from src.simulator_tools.simulator_utils import *
from src.simulator_tools.db_utils import *


class SimEvaluator:
    """
    Class responsible for bridging the agent and the simulator.
    Abstracts the type of agent and algorithm implemented from the workloads and data collection.
    """

    def __init__(self, trace_collector, workloads):
        self.trace_collector = trace_collector
        self.initial_config = self._read_init_config()
        self.current_config = self.initial_config
        self.workloads = workloads

    def _read_init_config(self):
        """Loads intitial configuration from initial_config/config.json."""

        config_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "initial_config",
            "config.json"
        )

        try:
            with open(config_path, 'r') as f:
                logging.info(
                    f"Loaded initial configuration from {config_path}")
                return json.load(f)
        except Exception as e:
            logging.error(
                f"Failed to load initial config from {config_path}: {e}")
            return {}

    def _run_workloads(self):
        """Executes all configured locust workloads sequentially."""

        workloads_dir = os.path.join(os.path.dirname(
            os.path.dirname(os.path.abspath(__file__))), "workloads")

        for wl in self.workloads:
            cmd = [
                sys.executable, "-m", "locust",
                "-f", wl.get("file"),
                "--headless",
                "--only-summary",
                "-u", str(wl.get("users")),
                "-r", str(wl.get("spawn_rate"))
            ]

            iterations = wl.get("iterations")
            run_time = wl.get("run_time")

            if iterations is not None:
                cmd.extend(["--iterations", str(iterations)])
            elif run_time is not None:
                cmd.extend(["-t", str(run_time)])

            logging.info(f"Running workload with command: {' '.join(cmd)}")
            try:
                # We use subprocess.run to wait for the workload to finish
                # Suppress stdout and stderr so it doesn't pollute the CLI
                # Add a 100-second timeout safety net
                subprocess.run(
                    cmd,
                    check=True,
                    cwd=workloads_dir,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    timeout=100
                )
                logging.info(
                    f"Workload {wl.get('file')} completed successfully.")
            except subprocess.TimeoutExpired as e:
                logging.error(
                    f"Workload {wl.get('file')} timed out.")
            except subprocess.CalledProcessError as e:
                logging.error(
                    f"Workload {wl.get('file')} execution failed: {e}")

    def reset_simulator(self):
        """Resets the simulator to the initial configuration (New Episode)."""

        SimInterface.inject_configuration(self.initial_config)
        self.current_config = self.initial_config
        return self.initial_config

    def evaluate_configuration(self, config) -> dict:
        """Injects a new configuration, runs the workloads and returns the resulting metrics."""

        # Reset metrics and DB so every config starts from the same point
        self.trace_collector.reset()
        try:
            DBManager.reset_database()
            DBManager.populate_database()
        except:
            return

        SimInterface.inject_configuration(config)
        self.current_config = config

        # TODO: run warmup?
        self._run_workloads()

        self.trace_collector.wait_for_data()
        return self.trace_collector.get_metrics()
