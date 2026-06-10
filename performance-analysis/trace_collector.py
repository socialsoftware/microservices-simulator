from collections import defaultdict
from concurrent import futures
import threading
from typing import Any, Dict, List

import grpc
import uvicorn
from fastapi import FastAPI
from opentelemetry.proto.collector.trace.v1 import (
    trace_service_pb2,
    trace_service_pb2_grpc,
)

# ======================
# TRACE MANAGER
# ======================


class TraceManager:
    """
    Stores, processes and serves trace data.
    This is the central component to be shared between servers and the RL agent.
    """

    def __init__(self):
        self._spans: Dict[str, Dict[str, Any]] = {}
        self._lock = threading.Lock()

    def reset(self):
        """Clears all stored spans."""

        with self._lock:
            self._spans.clear()
        print("Trace manager has been reset!")

    @staticmethod
    def _parse_otlp_value(value: Any) -> Any:
        """Recursively parses an OpenTelemetry AnyValue protobuf message into a Python type."""

        if value.HasField("string_value"):
            return value.string_value
        if value.HasField("int_value"):
            return value.int_value
        if value.HasField("double_value"):
            return value.double_value
        if value.HasField("bool_value"):
            return value.bool_value
        if value.HasField("bytes_value"):
            return value.bytes_value
        if value.HasField("array_value"):
            return [TraceManager._parse_otlp_value(v) for v in value.array_value.values]
        if value.HasField("kvlist_value"):
            return {kv.key: TraceManager._parse_otlp_value(kv.value) for kv in value.kvlist_value.values}
        return None

    @staticmethod
    def _parse_attributes(attributes: List[Any]) -> Dict[str, Any]:
        """Converts a list of OTLP KeyValue messages to a Python dictionary."""
        return {attr.key: TraceManager._parse_otlp_value(attr.value) for attr in attributes}

    def add_spans(self, resource_spans: List[Any]):
        """Adds new spans from a gRPC request to the storage."""

        spans_to_add = {}
        for r_span in resource_spans:
            for s_span in r_span.scope_spans:
                for span in s_span.spans:
                    span_id = span.span_id.hex()
                    spans_to_add[span_id] = {
                        "span_id": span_id,
                        "parent_id": span.parent_span_id.hex() if span.parent_span_id else None,
                        "name": span.name,
                        "start_ms": span.start_time_unix_nano / 1e6,
                        "end_ms": span.end_time_unix_nano / 1e6,
                        "duration": max(0, (span.end_time_unix_nano - span.start_time_unix_nano) / 1e6),
                        "attributes": TraceManager._parse_attributes(span.attributes),
                    }
        with self._lock:
            self._spans.update(spans_to_add)

    @staticmethod
    def _to_float(value: Any) -> float | None:
        """Converts a value to a float."""

        if value is None:
            return None
        if isinstance(value, (int, float)):
            return float(value)
        try:
            # Handle cases like "5.25ms"
            return float(str(value).replace("ms", "").strip())
        except (TypeError, ValueError):
            return None

    # curl http://127.0.0.1:8000/metrics
    def get_metrics(self) -> Dict[str, Any]:
        """Computes and returns the aggregated metrics."""

        with self._lock:
            # Work on a copy to ensure thread safety during iteration
            spans_view = self._spans.copy()

        func_metrics = defaultdict(lambda: {
                                   "execution_time": 0.0, "queue_time": 0.0, "delay_time": 0.0, "invocations": 0})
        ms_metrics = defaultdict(lambda: {
                                 "execution_time": 0.0, "queue_time": 0.0, "delay_time": 0.0, "invocations": 0})
        func_invocation_ids = defaultdict(set)
        executions = defaultdict(list)

        for span in spans_view.values():
            if exec_id := span["attributes"].get("executionId"):
                executions[exec_id].append(span)

        for exec_id, spans_in_exec in executions.items():
            func_name = next((s["attributes"]["functionality"] for s in spans_in_exec if "functionality" in s["attributes"]
                             and "command.name" not in s["attributes"]), "Unknown")
            if func_name == "Unknown":  # Fallback
                func_name = next((s["attributes"]["functionality"]
                                 for s in spans_in_exec if "functionality" in s["attributes"]), "Unknown")

            func_invocation_ids[func_name].add(exec_id)

            for s in spans_in_exec:
                attrs, duration = s["attributes"], s["duration"]
                is_func = "functionality" in attrs and "command.name" not in attrs and s["name"] not in [
                    "before", "after"]
                is_command = "command.name" in attrs
                is_delay = s["name"] in ["before", "after"]

                if is_func:
                    func_metrics[func_name]["execution_time"] += duration
                elif is_delay:
                    func_metrics[func_name]["delay_time"] += duration

                if queue_time_ms := self._to_float(attrs.get("queue time (ms)")):
                    if is_command:
                        func_metrics[func_name]["queue_time"] += queue_time_ms

                ms_name = attrs.get("microservice", "Unknown")
                if is_command:
                    ms_metrics[ms_name]["execution_time"] += duration
                    ms_metrics[ms_name]["invocations"] += 1
                    if queue_time_ms:
                        ms_metrics[ms_name]["queue_time"] += queue_time_ms
                elif is_delay:
                    if parent := spans_view.get(s["parent_id"]):
                        ms_name = parent["attributes"].get(
                            "microservice", "Unknown")
                    ms_metrics[ms_name]["delay_time"] += duration

        for name, metrics in func_metrics.items():
            metrics["useful_time"] = round(max(
                0, metrics["execution_time"] - metrics["queue_time"] - metrics["delay_time"]), 3)
            metrics["execution_time"] = round(metrics["execution_time"], 3)
            metrics["queue_time"] = round(metrics["queue_time"], 3)
            metrics["delay_time"] = round(metrics["delay_time"], 3)
            metrics["invocations"] = len(func_invocation_ids[name])

        for metrics in ms_metrics.values():
            metrics["useful_time"] = round(max(
                0, metrics["execution_time"] - metrics["queue_time"] - metrics["delay_time"]), 3)
            metrics["execution_time"] = round(metrics["execution_time"], 3)
            metrics["queue_time"] = round(metrics["queue_time"], 3)
            metrics["delay_time"] = round(metrics["delay_time"], 3)

        return {"functionalities": dict(func_metrics), "microservices": dict(ms_metrics)}


# ======================
# SERVER INTERFACES
# ======================

class TraceServiceReceiver(trace_service_pb2_grpc.TraceServiceServicer):
    """gRPC Service that receives traces from OpenTelemetry."""

    def __init__(self, trace_manager: TraceManager):
        self._trace_manager = trace_manager

    def Export(self, request, context):
        self._trace_manager.add_spans(request.resource_spans)
        return trace_service_pb2.ExportTraceServiceResponse()


# Create the single, shared instance of the TraceManager and the FastAPI app
trace_manager = TraceManager()
app = FastAPI(title="Trace Metrics Collector")


@app.get("/metrics")
def get_metrics_endpoint():
    """Endpoint to return the aggregated metrics."""
    return trace_manager.get_metrics()


@app.post("/reset")
def reset_collector_endpoint():
    """Endpoint to reset Trace Manager"""
    trace_manager.reset()
    return {"status": "ok", "message": "Collector state reset."}


def run():
    """Configures and runs the gRPC and FastAPI servers."""
    grpc_server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    trace_service_pb2_grpc.add_TraceServiceServicer_to_server(
        TraceServiceReceiver(trace_manager), grpc_server
    )
    grpc_server.add_insecure_port("[::]:4317")

    # Start the gRPC server in a background thread
    grpc_thread = threading.Thread(target=grpc_server.start, daemon=True)
    grpc_thread.start()
    print("gRPC server started on port 4317.")

    # Run FastAPI in the main thread to handle graceful shutdowns
    print("FastAPI server starting on port 8000.")
    try:
        uvicorn.run(app, host="0.0.0.0", port=8000)
    finally:
        grpc_server.stop(0)
        grpc_thread.join()
        print("Servers shut down.")


if __name__ == "__main__":
    run()
