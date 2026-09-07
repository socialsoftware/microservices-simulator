package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserver;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserverHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactWriterContext;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;

import java.util.List;
import java.util.Objects;

import static pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence.*;

/** Short-lived handle for one local gateway invocation, including unsuccessful retries. */
public final class ReadResponseObservation {
    private final ImpactEvidenceObserver observer;
    private final ImpactEvidence.Writer reader;
    private final String excludedRole;
    private final String transportCommandType;
    private final Command payload;
    private final boolean serialized;
    private final List<ReadResponseAdapter<?, ?>> adapters;

    private ReadResponseObservation(ImpactEvidenceObserver observer, Command command, boolean serialized,
                                    List<ReadResponseAdapter<?, ?>> adapters) {
        this.observer = observer;
        this.reader = ImpactWriterContext.current().orElse(null);
        this.excludedRole = ReadObservationContext.excludedRole();
        this.transportCommandType = command.getClass().getName();
        // Only the framework's exact, known wrapper has an audited payload contract.
        this.payload = command.getClass() == SagaCommand.class ? ((SagaCommand) command).getPayload() : command;
        this.serialized = serialized;
        this.adapters = adapters;
    }

    public static ReadResponseObservation begin(Command command, boolean serialized,
                                                 List<ReadResponseAdapter<?, ?>> adapters) {
        ImpactEvidenceObserver observer = ImpactEvidenceObserverHolder.current();
        // Do not recursively observe observer callbacks, even if they invoke the gateway.
        if ("OBSERVER_CALLBACK".equals(ReadObservationContext.excludedRole())) return null;
        if (!ImpactEvidenceObserverHolder.isReadObservationEnabled(observer)) return null;
        try {
            return new ReadResponseObservation(observer, command, serialized, adapters);
        } catch (RuntimeException failure) {
            ImpactEvidenceObserverHolder.retainReadFailure(observer, "READ_OBSERVATION_START_FAILED", failure);
            return null;
        }
    }

    public void delivered(Object response) {
        try {
            observe(response);
        } catch (RuntimeException failure) {
            ImpactEvidenceObserverHolder.retainReadFailure(observer, "READ_ADAPTER_FAILED", failure);
            publish(response, Outcome.DELIVERED_INVALID, null, null, null, "READ_ADAPTER_FAILED");
        }
    }

    public void failed(Throwable failure) {
        String exclusion = exclusionReason();
        String gap = readerGap();
        if (exclusion != null) publish(null, Outcome.EXCLUDED, null, null, null, exclusion);
        else if (gap != null) publish(null, Outcome.FAILED_INVALID, null, null, null, gap);
        else publish(null, Outcome.FAILED, null, null, null, failure.getClass().getName());
    }

    private void observe(Object response) {
        String exclusion = exclusionReason();
        if (exclusion != null) {
            publish(response, Outcome.EXCLUDED, null, null, null, exclusion);
            return;
        }
        String gap = readerGap();
        if (gap != null) {
            publish(response, Outcome.DELIVERED_INVALID, null, null, null, gap);
            return;
        }
        if (payload == null) {
            publish(response, Outcome.DELIVERED_INVALID, null, null, null, "MISSING_COMMAND_PAYLOAD");
            return;
        }
        List<ReadResponseAdapter<?, ?>> commandAdapters = adapters.stream()
                .filter(adapter -> adapter.commandType() == payload.getClass()).toList();
        if (commandAdapters.isEmpty()) {
            publish(response, Outcome.DELIVERED_UNMAPPED, null, null, null, "NO_READ_ADAPTER");
            return;
        }
        List<ReadResponseAdapter<?, ?>> matches = commandAdapters.stream()
                .filter(adapter -> response != null && adapter.responseType() == response.getClass()).toList();
        if (matches.size() != 1) {
            publish(response, Outcome.DELIVERED_INVALID, null, null, null,
                    matches.isEmpty() ? "UNSUPPORTED_RESPONSE_TYPE" : "AMBIGUOUS_READ_ADAPTER");
            return;
        }
        extract(matches.getFirst(), response);
    }

    private String exclusionReason() {
        if (excludedRole != null) return excludedRole;
        if (reader != null && ("SETUP".equals(reader.kind()) || "OBSERVER".equals(reader.kind())
                || "PROBE".equals(reader.kind()) || "EVENT".equals(reader.kind())
                || "EVENT_CONSUMER".equals(reader.kind())
                || ("SAGA".equals(reader.kind()) && "RECOVERY".equals(reader.phase())))) {
            return "READER_ROLE_EXCLUDED";
        }
        return null;
    }

    private String readerGap() {
        if (reader == null || !"SAGA".equals(reader.kind()) || !"FORWARD".equals(reader.phase())
                || reader.eventId() != null || blank(reader.executionAttemptId()) || blank(reader.workloadPlanId())
                || blank(reader.sagaInstanceId()) || blank(reader.actionId())
                || blank(reader.functionalityName()) || blank(reader.stepName())) {
            return "MISSING_READER_ATTRIBUTION";
        }
        return Objects.equals(reader, ImpactWriterContext.current().orElse(null))
                ? null : "READER_ATTRIBUTION_CHANGED";
    }

    private <C extends Command, R> void extract(ReadResponseAdapter<C, R> adapter, Object response) {
        Contract contract = new Contract(adapter.contractId(), adapter.contractVersion(),
                adapter.commandType().getName(), adapter.responseType().getName(),
                adapter.aggregateType(), adapter.runtimeType());
        if (blank(contract.id()) || blank(contract.version()) || blank(contract.aggregateType())
                || blank(contract.runtimeType())) {
            publish(response, Outcome.DELIVERED_INVALID, contract, null, null, "INVALID_ADAPTER_CONTRACT");
            return;
        }
        R typed = adapter.responseType().cast(response);
        Integer aggregateId = adapter.aggregateId(typed);
        Long version = adapter.version(typed);
        ImpactEvidence.AggregateIdentity identity = new ImpactEvidence.AggregateIdentity(
                contract.aggregateType(), aggregateId);
        String reason = aggregateId == null ? "MISSING_RETURNED_IDENTITY"
                : version == null ? "MISSING_RETURNED_REVISION" : null;
        publish(response, reason == null ? Outcome.DELIVERED : Outcome.DELIVERED_INVALID,
                contract, identity, version, reason);
    }

    private void publish(Object response, Outcome outcome, Contract contract,
                         ImpactEvidence.AggregateIdentity identity, Long version, String reason) {
        ImpactEvidenceObserverHolder.readResponse(observer, new Observation(reader, transportCommandType,
                payload == null ? null : payload.getClass().getName(),
                response == null ? null : response.getClass().getName(), serialized,
                outcome, contract, identity, version, reason));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
