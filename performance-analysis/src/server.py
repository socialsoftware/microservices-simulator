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

# Create the single, shared instance of the TraceManager
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


def start_grpc_server():
    """Configures and runs the gRPC server in a background thread."""

    # Start gRPC Server
    grpc_server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    trace_service_pb2_grpc.add_TraceServiceServicer_to_server(
        TraceServiceReceiver(trace_manager), grpc_server
    )
    grpc_server.add_insecure_port("[::]:4319")

    grpc_thread = threading.Thread(target=grpc_server.start, daemon=True)
    grpc_thread.start()
    print("gRPC server started on port 4319.")

    return grpc_server


def interactive_cli():
    """Interactive CLI to manage the RL Server."""

    print("\n--- RL Server CLI ---")
    print("Available commands:")
    print("  read  - Print current metrics")
    print("  reset - Reset trace manager metrics")
    print("  train - Start the agent training loop")
    print("  exit  - Stop the server and exit")

    while True:
        try:
            cmd = input("rl-server> ").strip().lower()
            if not cmd:
                continue

            if cmd == "read":
                metrics = trace_manager.get_metrics()
                print(json.dumps(metrics, indent=2))
            elif cmd == "reset":
                trace_manager.reset()
            elif cmd.startswith("train"):
                if cmd == "train ppo":
                    start_training(trace_manager, "ppo")
                else:
                    start_training(trace_manager, "test")
            elif cmd in ["exit", "quit"]:
                print("Exiting...")
                break
            else:
                print(f"Unknown command: '{cmd}'")
        except EOFError:
            break
        except KeyboardInterrupt:
            print("\nExiting...")
            break
        except Exception as e:
            print(f"Error executing command: {e}")


# source venv/Scripts/activate
if __name__ == "__main__":
    server = start_grpc_server()

    try:
        interactive_cli()
    finally:
        print("Shutting down gRPC server...")
        server.stop(0)
