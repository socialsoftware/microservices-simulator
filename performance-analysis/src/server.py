import os
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["OPENBLAS_NUM_THREADS"] = "1"
os.environ["NUMEXPR_NUM_THREADS"] = "1"
os.environ["VECLIB_MAXIMUM_THREADS"] = "1"

import threading
from concurrent import futures
import json
import grpc
from opentelemetry.proto.collector.trace.v1 import (
    trace_service_pb2,
    trace_service_pb2_grpc,
)

from src.trace_collection.trace_collector import TraceManager
from src.agents.train import start_training
from src.agents.evaluation.eval_agents import start_evaluation
from src.agents.evaluation.eval_configurations import start_baseline_eval

import logging
logging.basicConfig(level=logging.WARNING)

DEFAULT_CLI_PORT = 4320
trace_manager = TraceManager()

# ======================
# gRPC RECEIVER
# ======================


class TraceServiceReceiver(trace_service_pb2_grpc.TraceServiceServicer):
    """gRPC Service that receives traces from OpenTelemetry."""

    def __init__(self, tm: TraceManager):
        self._trace_manager = tm

    def Export(self, request, context):
        self._trace_manager.add_spans(request.resource_spans)
        return trace_service_pb2.ExportTraceServiceResponse()

# ======================
# SERVER & CLI
# ======================


def start_grpc_server(port=4319, tm=None):
    """Configures and runs the gRPC server in a background thread."""

    if tm is None:
        tm = trace_manager

    # Start gRPC Server
    grpc_server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    trace_service_pb2_grpc.add_TraceServiceServicer_to_server(
        TraceServiceReceiver(tm), grpc_server
    )
    grpc_server.add_insecure_port(f"0.0.0.0:{port}")

    grpc_thread = threading.Thread(target=grpc_server.start, daemon=True)
    grpc_thread.start()
    print(f"gRPC server started on port {port}.")

    return grpc_server


def interactive_cli():
    """Interactive CLI to manage the RL Server."""

    global server
    
    print("\n--- RL Server CLI ---")
    print("Available commands:")
    print("  read     - Print current metrics")
    print("  reset    - Reset trace manager metrics")
    print("  train    - Start the agent training loop (e.g. train {ppo, test})")
    print("  eval     - Evaluate a trained model (e.g., eval {ppo} [MODEL_PATH])")
    print("  baseline - Evaluate a static configuration baseline (e.g., baseline [WORKLOAD_PATH] [JSON_CONFIG_PATH])")
    print("  debug    - Toggle debug logging")
    print("  exit     - Stop the server and exit")

    while True:
        try:
            cmd = input("rl-server> ").strip().lower()
        except EOFError:
            break
        except KeyboardInterrupt:
            print("\nExiting...")
            break

        try:
            if not cmd:
                continue

            if cmd == "read":
                metrics = trace_manager.get_metrics()
                print(json.dumps(metrics, indent=2))
            elif cmd == "reset":
                trace_manager.reset()
                print("Trace Manager Cleared!")
            elif cmd.startswith("train"):
                server.stop(0)
                try:
                    if cmd == "train ppo":
                        start_training("ppo")
                    else:
                        start_training("test")
                finally:
                    server = start_grpc_server(DEFAULT_CLI_PORT)
            elif cmd.startswith("eval"):
                parts = cmd.split(" ", 2)
                if len(parts) < 3:
                    print("Usage: eval {ppo} <path_to_model.zip>")
                else:
                    trace_manager.reset()
                    try:
                        start_evaluation(trace_manager, parts[1], parts[2])
                    finally:
                        trace_manager.reset()
            elif cmd.startswith("baseline"):
                parts = cmd.split(" ", 2)
                if len(parts) < 3:
                    print("Usage: baseline <workload_path> <config_json_path>")
                else:
                    trace_manager.reset()
                    try:
                        start_baseline_eval(trace_manager, parts[1], parts[2])
                    finally:
                        trace_manager.reset()
            elif cmd == "debug":
                logger = logging.getLogger()
                if logger.level == logging.INFO:
                    logger.setLevel(logging.WARNING)
                    print("Logging level set to WARNING")
                else:
                    logger.setLevel(logging.INFO)
                    print("Logging level set to INFO")
            elif cmd in ["exit", "quit"]:
                print("Exiting...")
                break
            else:
                print(f"Unknown command: '{cmd}'")
        except Exception as e:
            import traceback
            traceback.print_exc()
            print(f"Error executing command: {e}")


if __name__ == "__main__":
    server = start_grpc_server(DEFAULT_CLI_PORT)

    try:
        interactive_cli()
    finally:
        print("Shutting down gRPC server...")
        server.stop(0)
