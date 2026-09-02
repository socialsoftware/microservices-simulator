package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "kind", "sequence", "timestamp", "thread", "test", "saga", "invocation",
        "step", "input", "phase", "outcome", "error", "command", "access", "violation"})
public record DynamicObservation(
        String id,
        String kind,
        long sequence,
        String timestamp,
        String thread,
        TestIdentity test,
        String saga,
        String invocation,
        String step,
        String input,
        String phase,
        String outcome,
        ErrorDetail error,
        CommandDetail command,
        AccessDetail access,
        ViolationDetail violation) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TestIdentity(String execution, @com.fasterxml.jackson.annotation.JsonProperty("class") String testClass,
                               String method) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(String type, String message) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CommandDetail(String type, Map<String, Object> fields) {
        public CommandDetail {
            fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AccessDetail(String aggregate, String id, String mode) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ViolationDetail(String type, String message) { }
}
