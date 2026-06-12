# Microservices Performance Analysis & RL Server

This directory contains the Python infrastructure for running performance workloads, collecting OpenTelemetry traces, and training Reinforcement Learning (RL) or Genetic Algorithm (GA) agents against the Microservices Simulator.

## Architecture Overview

The system is decoupled for maximum performance and ease of development:
1. **The Environment (Docker):** The microservices simulator, databases, and OpenTelemetry Collector run inside Docker.
2. **The Agent (Local):** The Python RL Agent and Trace Collector run directly on your host machine. They communicate with the simulator via HTTP endpoints and receive telemetry via a local gRPC server.

## Project Structure

```text
performance-analysis/
├── otel-collector-config.yaml # OTel config routing traces to the host
├── requirements.txt           # Python dependencies
├── pyproject.toml             # Build system configuration
└── src/                       # Main Python package
    ├── server.py              # Main entry point (Interactive CLI)
    ├── trace_collection/      # gRPC trace reception and metrics processing
    ├── simulator_tools/       # HTTP and DB utilities to control the Java simulator
    ├── workloads/             # Locust workload definitions
    ├── initial_config/        # Baseline JSON configurations to inject on reset
    └── agent/                 # Agent orchestration logic
        ├── agent.py           # The main agent loop
        └── simulator_evaluator.py # Handles the inject -> run locust -> get metrics loop
```

## Quick Start Guide

Follow these steps to set up the project on a new machine.

### 1. Start the Simulator Environment
From the root of the repository (`microservices-simulator`), start the Docker stack. This will spin up the Java simulator, PostgreSQL, Jaeger, and the OTel Collector.
```bash
docker compose up quizzes-rl -d
```

### 2. Setup the Python Environment
Navigate to this directory and create a virtual environment:
```bash
cd performance-analysis
python -m venv venv
```

### 3. Activate the Virtual Environment
Activate the environment so your terminal uses the isolated dependencies.

*   **Git Bash / Linux / macOS:**
    ```bash
    source venv/Scripts/activate  # Windows (Git Bash)
    # OR
    source venv/bin/activate      # Mac/Linux
    ```

### 4. Install Dependencies
Install the required packages. The `requirements.txt` file includes `-e .`, which will install this project in "editable mode," allowing all the `src.*` imports to work correctly.
```bash
pip install -r requirements.txt
```

### 5. Run the Server
Start the interactive CLI server. This will boot the gRPC receiver in the background and drop you into a command prompt.
```bash
python src/server.py
```

### CLI Commands
Once the server is running, you can use the following commands at the `rl-server>` prompt:
*   `read`: Prints the current structured metrics (from the last workload) as formatted JSON.
*   `reset`: Flushes all stored traces and resets the internal Trace Manager.
*   `train`: Triggers the main agent loop.
*   `exit`: Safely shuts down the background gRPC server and closes the application.