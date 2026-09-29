# Microservices Performance Analysis & RL Server

This directory contains the Python infrastructure for running the Reinforcement Learning Optimization (RLO) Framework.

---

## Architecture Overview

1. **The Environment (Docker):** The Java microservices simulator backend (e.g. `quizzes`), databases (embedded H2 and PostgreSQL), and observability stack (Jaeger and OpenTelemetry Collector) run in Docker.
2. **The Agent & Server (Host):** The Python RL Agent, Trace Collector, and interactive CLI run directly on the host machine. They control the simulator via HTTP endpoints and receive OpenTelemetry trace spans in real-time via an in-memory gRPC server.

---

## Directory Structure

```text
performance-analysis/
├── otel-collector-config.yaml           # OTel collector routing metrics
├── docker-compose-parallel-training.yml # Docker Compose configuration
├── start_parallel.sh                    # Helper script to launch simulator instances
├── stop_parallel.sh                     # Helper script to tear down simulator instances
├── requirements.txt                     # Python dependencies
├── pyproject.toml                       # Build system project configuration
├── models/                              # Trained models and evaluation outputs
│   ├── checkpoints/                     # Periodic training checkpoints
│   └── eval_results/                    # Agent-optimized configurations
├── tensorboard_logs/                    # TensorBoard logs for training and baseline evaluations
└── src/                                 # Main Python package
    ├── server.py                        # Central interactive CLI and gRPC server entry point
    ├── trace_collection/                #
    │   └── trace_collector.py           # In-memory TraceManager and gRPC TraceService receiver
    ├── simulator_tools/                 # Utilities for interacting with the Java simulator
    │   ├── config_utils.py              # Configuration parsing, manipulation, and path resolution
    │   ├── h2_utils.py                  # Database state setup and table resets via H2 TCP
    │   └── simulator_utils.py           # HTTP client to update microservice placements & capacities
    ├── workloads/                       # Locust workload definitions
    │   └── quizzes/                     # Quizzes application workloads
    │       └── workload_class.py        # Base Locust workload Class
    ├── initial_config/                  # Baseline configurations and seed data
    │   ├── config.json                  # Default microservice placement and node capacities
    │   ├── baseline_data.sql            # SQL seed script to restore default database state
    │   └── baseline_data.py             # Script to initialize baseline database data
    └── agents/                          # Agent training, environments, and evaluation
        ├── train.py                     # Training entry point
        ├── rl/                          # Reinforcement learning components
        │   ├── ppo_agent.py             # MaskablePPO training loop
        │   ├── ppo_config.yaml          # Hyperparameters, workload settings, and training config
        │   ├── eval_ppo.py              # Deterministic model evaluation to optimize configurations
        │   ├── environments/            # Gymnasium environment
        │   ├── action_spaces/           # Actions available to the agent
        │   ├── observation_spaces/      # State representations strategies
        │   └── rewards/                 # Reward functions
        ├── evaluation/                  # Benchmarking and comparative evaluation
        │   ├── eval_agents.py           # Evaluation dispatcher
        │   └── eval_configurations.py   # Baseline static configuration evaluator
        └── utils/                       # Shared agent utilities
            ├── env_setup.py             # Environment factory (single and parallel environments)
            ├── simulation_runner.py     # SimRunner: workload execution and metric collection bridge
            ├── tensorboard_metrics.py   # Metric aggregation for TensorBoard logging
            └── rendering.py             # Terminal rendering of microservice placements
```

---

## Quick Start Guide

### 1. Start the Simulator Environment (Docker)

Start the simulator container(s) using the [`start_parallel.sh`](start_parallel.sh) helper script:

```bash
# Start 1 instance in debug mode (with Jaeger UI on port 16687 and OTel Collector):
./start_parallel.sh 1 train-debug

# Or start 1 instance in standard training mode (direct OTLP export to host):
./start_parallel.sh 1 train

# Or start N instances for parallel environment training:
./start_parallel.sh 4 train
```
*(Instance $i$ automatically binds: Gateway on `http://localhost:$((8080+i))` (`8081` for $i=1$), H2 on port `$((1521+i))` (`1522` for $i=1$), and trace gRPC on port `$((4319+i))` (`4320` for $i=1$)).*

### 2. Setup the Python Virtual Environment

Navigate to this directory and create/activate the virtual environment:

```bash
cd performance-analysis
python3 -m venv venv
source venv/bin/activate
```

### 3. Install Dependencies

Install the required Python packages in editable mode:

```bash
pip install -r requirements.txt
```

### 4. Launch the Interactive CLI

Run the server script:

```bash
python src/server.py
```
This starts the background gRPC trace receiver (port `4320` by default) and drops into the `rl-server>` interactive prompt.

### 5. Stop the Simulator Environment

When finished, tear down the running container instances and clean up volumes using [`stop_parallel.sh`](stop_parallel.sh):

```bash
# Stop 1 instance (default):
./stop_parallel.sh 1

# Or stop N parallel instances:
./stop_parallel.sh 4
```

---

## Interactive CLI Commands

Inside the `rl-server>` prompt, the following commands are available:

| Command | Syntax | Description |
|---|---|---|
| `read` | `read` | Prints the current accumulated trace metrics (functionalities & microservices) as formatted JSON. |
| `reset` | `reset` | Flushes all stored traces and resets the in-memory Trace Manager. |
| `train` | `train {ppo, test}` | Starts the training loop (`train ppo` trains PPO with parallel environments; `train test` runs a test loop). |
| `eval` | `eval {ppo} <model_path> <workload_path> <config_path>` | Evaluates a trained model deterministically against worst-case traffic, finds the optimal configuration, and saves it to `models/eval_results/optimized_config.json`. |
| `baseline` | `baseline <workload_path> <config_path>` | Benchmarks a static configuration across an episode under deterministic conditions, logs to TensorBoard (`baseline_eval_N`), prints a Mean/Median summary table, and saves `summary.txt`. |
| `debug` | `debug` | Toggles Python logging verbosity between `INFO` and `WARNING`. |
| `exit` / `quit` | `exit` | Shuts down the gRPC server and exits the application. |

---

## Evaluation & Benchmarking Workflow

The standard evaluation methodology compares the default system baseline against the configuration discovered by a trained RL agent under identical, controlled conditions.

### Parameter & Configuration Sources
* **`train`**: Everything loaded from default paths (`config.json` for base topology, `ppo_config.yaml` for training and traffic).
* **`eval`**: Model checkpoint, workload, and `config.json` loaded from CLI inputs; traffic parameters loaded from `ppo_config.yaml` (locked to deterministic load).
* **`baseline`**: Workload and `config.json` loaded from CLI inputs; traffic parameters loaded from `ppo_config.yaml` (locked to deterministic load).
* *Note*: Traffic parameters are always loaded from `ppo_config.yaml` for simplification.
* *Note*: To better understand the structure and parameters of `config.json`, refer to the [Root Configuration Reference](../README.md#configuration-reference).

### Path Resolution Shortcuts
You do not need to provide full filesystem paths:
* **Workload Paths**: Can be specified relative to `src/workloads/` (e.g. `quizzes/steady_2_wl.py`).
* **Config Paths**: Lookups automatically check the current working directory, `src/initial_config/`, and `models/eval_results/` (e.g. `config.json` or `optimized_config.json`).

### Step-by-Step Evaluation Flow

1. **Evaluate the Trained Agent**:
   Run `eval` with your model checkpoint, workload, and base configuration. The agent deterministically discovers optimal placements and capacities:
   ```text
   rl-server> eval ppo models/checkpoints/ppo_model.zip quizzes/steady_2_wl.py config.json
   ```
   * Automatically resets the H2 database to a clean baseline state.
   * Locks traffic parameters to worst-case deterministic load.
   * Saves the final configuration directly to `models/eval_results/optimized_config.json`.

2. **Benchmark Default Baseline**:
   Run `baseline` on the initial default configuration:
   ```text
   rl-server> baseline quizzes/steady_2_wl.py config.json
   ```
   * Automatically resets the database.
   * Runs the workload for 1 full episode (15 steps) under deterministic worst-case traffic.
   * Logs scalars to `tensorboard_logs/baseline_eval_1`.
   * Displays the final **Mean and Median** summary table in the console and saves `summary.txt`.

3. **Benchmark Agent-Optimized Configuration**:
   Run `baseline` on the agent's discovered configuration:
   ```text
   rl-server> baseline quizzes/steady_2_wl.py optimized_config.json
   ```
   * Automatically increments the TensorBoard log directory to `tensorboard_logs/baseline_eval_2` (preventing trace/metric collisions).
   * Displays the **Mean and Median** summary table to directly compare performance gains.

---

## Experimental Data (`data/`)

All thesis experiments are organized under `data/<model_experiment>/` (one folder per RL model):
* **`inputs/`**: Training configuration (`ppo_config.yaml`) and starting topology (`initial_config.json`, which may vary per episode).
* **`outputs/`**: Contains `trained_model.zip`, training logs (`train_tensorlog/`), and an `experiments/` directory.
  * **`experiments/<experiment_name>/`**: Contains initial and agent-optimized configurations (`initial_config.json`, `optimized_config.json`) alongside their benchmark logs (`initial_tensorlog/`, `optimized_tensorlog/`).
* *Note*: Any directory ending in `_tensorlog` contains TensorBoard event logs produced by evaluating the corresponding configuration.

---

## Viewing Results in TensorBoard

To view metrics (learning curves, queue latencies, delay times, invocation throughput), pass the target log folder directly to `--logdir`:

```bash
# View model training curves:
tensorboard --logdir data/static_agent/outputs/train_tensorlog

# Compare initial vs optimized benchmarks for an experiment:
tensorboard --logdir data/static_agent/outputs/experiments/steady_2

# View live runtime evaluation logs:
tensorboard --logdir tensorboard_logs
```
Open `http://localhost:6006` in your browser.