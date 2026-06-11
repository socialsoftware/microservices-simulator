from collections import defaultdict
import threading
import time
from typing import Any, Dict, List


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
        self._lock = threading.Condition(threading.Lock())
        self._last_span_time = 0.0

    def reset(self):
        """Clears all stored spans."""

        with self._lock:
            self._spans.clear()
            self._last_span_time = 0.0
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
            self._last_span_time = time.time()
            self._lock.notify_all()

    # TODO: Change total_timeout to a decent value
    def wait_for_data(self, wait_window=0.1, total_timeout=100.0):
        """
        Blocks until data has arrived and no new spans have been received for 'wait_window' seconds.
        """

        start_time = time.time()
        with self._lock:
            # Wait for ANY data to arrive first
            while not self._spans and (time.time() - start_time) < total_timeout:
                remaining = total_timeout - (time.time() - start_time)
                self._lock.wait(remaining)

            if not self._spans:
                return False

            # Wait to stop receiving
            while (time.time() - self._last_span_time) < wait_window:
                remaining = wait_window - (time.time() - self._last_span_time)
                if remaining <= 0:
                    break
                self._lock.wait(remaining)
                if (time.time() - start_time) > total_timeout:
                    break
        return True

    @staticmethod
    def _to_float(value: Any) -> float | None:
        """Converts a value to a float."""

        if value is None:
            return None
        if isinstance(value, (int, float)):
            return float(value)
        try:
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
