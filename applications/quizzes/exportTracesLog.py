import json
import sys
from datetime import datetime
from time import sleep
from opentelemetry import trace
from opentelemetry.sdk.resources import SERVICE_NAME, Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, SimpleSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace.export import ConsoleSpanExporter
from opentelemetry.trace import set_span_in_context, use_span
from opentelemetry.trace import SpanKind
from dateutil import parser as date_parser

# Setup OpenTelemetry tracing
trace.set_tracer_provider(
    TracerProvider(
        resource=Resource.create({SERVICE_NAME: "test-application"})
    )
)
tracer = trace.get_tracer(__name__)
span_processor = SimpleSpanProcessor(OTLPSpanExporter(endpoint="http://localhost:4317", insecure=True))  # Immediate export
trace.get_tracer_provider().add_span_processor(span_processor)

# Keep track of active spans
active_functionality_spans = {}
active_step_spans = {}

def timestamp_to_nanoseconds(timestamp):
    """Convert datetime timestamp to nanoseconds as integer"""
    return int(timestamp.timestamp() * 1_000_000_000)

def extract_name(message):
    parts = message.split(":")
    if len(parts) < 2:
        return "unknown"
    name_part = parts[1].strip().split(" ")
    return name_part[0]

def extract_functionality(message):
    # Look for "from functionality" pattern and extract the name before "FunctionalitySagas"
    idx = message.find("from functionality")
    if idx == -1:
        return "unknown"
    parts = message[idx:].split()
    if len(parts) < 3:
        return "unknown"
    
    # Extract functionality name and remove "FunctionalitySagas" suffix
    func_name = parts[2]  # e.g., "CreateCourseExecutionFunctionalitySagas"
    if func_name.endswith("FunctionalitySagas"):
        # Convert "CreateCourseExecutionFunctionalitySagas" to "createCourseExecution"
        base_name = func_name.replace("FunctionalitySagas", "")
        # Convert from PascalCase to camelCase
        if base_name:
            return base_name[0].lower() + base_name[1:]
    
    return func_name

def process_line(line):
    global active_functionality_spans, active_step_spans

    data = json.loads(line)
    timestamp = date_parser.parse(data["@timestamp"])
    message = data["message"]
    
    timestamp_ns = timestamp_to_nanoseconds(timestamp)

    if message.startswith("START EXECUTION FUNCTIONALITY"):
        name = extract_name(message)
        span = tracer.start_span(
            f"Functionality: {name}",
            kind=SpanKind.INTERNAL,
            start_time=timestamp_ns
        )
        active_functionality_spans[name] = span

    elif message.startswith("END EXECUTION FUNCTIONALITY") or message.startswith("ABORT EXECUTION FUNCTIONALITY"):
        name = extract_name(message)
        span = active_functionality_spans.pop(name, None)
        if span:
            span.end(end_time=timestamp_ns)

    elif message.startswith("START EXECUTION STEP"):
        name = extract_name(message)
        functionality = extract_functionality(message)
        parent_span = active_functionality_spans.get(functionality)

        if parent_span:
            ctx = set_span_in_context(parent_span)
            span = tracer.start_span(
                f"Step: {name}",
                context=ctx,
                kind=SpanKind.INTERNAL,
                start_time=timestamp_ns
            )
        else:
            span = tracer.start_span(
                f"Step: {name}",
                kind=SpanKind.INTERNAL,
                start_time=timestamp_ns
            )
        active_step_spans[name] = span

    elif message.startswith("END EXECUTION STEP"):
        name = extract_name(message)
        span = active_step_spans.pop(name, None)
        if span:
            span.end(end_time=timestamp_ns)

def main(log_file_path):
    print(f"Processing log file: {log_file_path}")
    line_count = 0
    
    with open(log_file_path, "r") as f:
        for line in f:
            line = line.strip()
            if line:
                line_count += 1
                try:
                    process_line(line)
                except Exception as e:
                    print(f"Error processing line {line_count}: {line}\n{e}")

    print(f"Processed {line_count} lines")
    print(f"Active functionality spans: {len(active_functionality_spans)}")
    print(f"Active step spans: {len(active_step_spans)}")
    
    # Force flush before sleep
    print("Flushing spans...")
    trace.get_tracer_provider().force_flush(timeout_millis=10000)
    
    # Give time for the spans to be exported
    print("Waiting for export...")
    sleep(5)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python exportTracesLog.py <log-file-path>")
        sys.exit(1)

    main(sys.argv[1])