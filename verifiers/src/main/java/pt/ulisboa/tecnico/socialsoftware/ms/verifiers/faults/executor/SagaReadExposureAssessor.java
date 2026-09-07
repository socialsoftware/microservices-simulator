package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureReport.*;

/** Deterministic creation/delivery/explicit-compensation proof; no application-specific branches. */
final class SagaReadExposureAssessor {
    SagaReadExposureReport assess(ScenarioExecutionReport execution, String attemptId, String workloadId,
                                  String scenarioId, boolean started, String unavailableReason,
                                  boolean baselineCovered, List<ReadResponseEvidence.Contract> contracts,
                                  List<Revision> baseline, SourceContract sources, List<Write> writes,
                                  List<Call> calls, List<Gap> collectionGaps, List<ArtifactReference> artifacts) {
        List<Action> actions = execution.actualActions().stream().map(action -> Action.from(action, execution))
                .sorted(Comparator.comparingInt(Action::actualPosition).thenComparing(Action::id,
                        Comparator.nullsFirst(String::compareTo))).toList();
        Index index = new Index(attemptId, workloadId, sources, baseline, writes, actions);
        boolean completeExecution = Set.of("SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED")
                .contains(execution.terminalStatus())
                && Set.of("EXACT", "DEVIATED").contains(String.valueOf(execution.scheduleConformance()));
        List<Gap> gaps = new ArrayList<>(collectionGaps);
        boolean ownedExecution = Objects.equals(attemptId, execution.executionAttemptId())
                && Objects.equals(workloadId, execution.workloadPlanId())
                && Objects.equals(scenarioId, execution.faultScenarioId());
        if (!ownedExecution) gaps.add(new Gap(0, "EXECUTION", null, "EXECUTION_ATTRIBUTION_MISMATCH"));
        List<ReadResponseEvidence.Contract> declared = contracts.stream().filter(this::validContract).toList();
        if (declared.size() != contracts.size()) gaps.add(new Gap(0, "SCOPE", null, "INVALID_DECLARED_CONTRACT"));
        Map<List<String>, Long> pairs = declared.stream().collect(Collectors.groupingBy(
                contract -> List.of(contract.commandType(), contract.responseType()), Collectors.counting()));
        Map<List<String>, Long> ids = declared.stream().collect(Collectors.groupingBy(
                contract -> List.of(contract.id(), contract.version()), Collectors.counting()));
        List<ReadResponseEvidence.Contract> validContracts = declared.stream().filter(contract ->
                pairs.get(List.of(contract.commandType(), contract.responseType())) == 1
                        && ids.get(List.of(contract.id(), contract.version())) == 1).toList();
        if (validContracts.size() != declared.size()) gaps.add(new Gap(0, "SCOPE", null, "AMBIGUOUS_DECLARED_CONTRACT"));
        boolean usable = started && unavailableReason == null && !validContracts.isEmpty()
                && !actions.isEmpty() && ownedExecution;
        String unavailable = unavailableReason != null ? unavailableReason : !started ? "MEASUREMENT_NOT_STARTED"
                : validContracts.isEmpty() ? "NO_USABLE_ADAPTER_SCOPE" : actions.isEmpty() ? "NO_MEASURED_ACTIONS"
                : !ownedExecution ? "EXECUTION_ATTRIBUTION_MISMATCH" : null;
        List<Assessment> assessments = new ArrayList<>();
        Map<String, Finding> findings = new LinkedHashMap<>();
        for (Call call : calls.stream().sorted(Comparator.comparingLong(Call::order)).toList()) {
            Decision decision = usable ? evaluate(call, index, validContracts, baselineCovered,
                    completeExecution, collectionGaps) : Decision.unknown(unavailable);
            String findingId = null;
            if (decision.creation != null) {
                Write creation = decision.creation;
                Write deletion = decision.deletion;
                String reader = call.observation().reader().sagaInstanceId();
                findingId = "exposure:" + UUID.nameUUIDFromBytes((attemptId + "\n" + creation.id() + "\n"
                        + deletion.id() + "\n" + reader).getBytes(StandardCharsets.UTF_8));
                Finding previous = findings.get(findingId);
                List<String> deliveries = new ArrayList<>(previous == null ? List.of() : previous.deliveryIds());
                deliveries.add(call.id());
                Action recovery = index.action(deletion.writer());
                findings.put(findingId, new Finding(findingId, "OBSERVED", creation.revision().identity(),
                        creation.revision().runtimeType(), creation.revision().version(), deletion.revision().version(),
                        creation.writer().sagaInstanceId(), reader, creation.id(), deletion.id(),
                        recovery.sourceScheduledStepId(), recovery.checkpointId(), deliveries));
            }
            assessments.add(new Assessment(call.id(), decision.verdict, decision.reason, findingId));
            if ("UNKNOWN".equals(decision.verdict)) gaps.add(new Gap(call.order(), "ASSESSMENT", call.id(), decision.reason));
        }
        if (usable && !completeExecution) gaps.add(new Gap(index.lastOrder, "EXECUTION", execution.hardStopActionId(),
                "INCOMPLETE_EXECUTION_PREFIX"));
        String coverage = !usable ? "UNAVAILABLE" : gaps.isEmpty() ? "COMPLETE_WITHIN_SCOPE" : "PARTIAL";
        return new SagaReadExposureReport(null, attemptId, workloadId, scenarioId, execution.terminalStatus(),
                execution.scheduleConformance(), completeExecution ? "COMPLETE" : actions.isEmpty() ? "NOT_MEASURED"
                : "INCOMPLETE", coverage, !usable ? unavailable : gaps.isEmpty() ? null : "COVERAGE_GAPS",
                usable ? findings.size() : null, SCOPE, EXCLUDED_PATHS, contracts, artifacts, baselineCovered,
                baseline, sources, actions, writes.stream().sorted(Comparator.comparingLong(Write::order)).toList(),
                calls.stream().sorted(Comparator.comparingLong(Call::order)).toList(), assessments,
                List.copyOf(findings.values()), gaps);
    }

    private Decision evaluate(Call call, Index index, List<ReadResponseEvidence.Contract> contracts,
                              boolean baselineCovered, boolean completeExecution, List<Gap> gaps) {
        ReadResponseEvidence.Observation read = call.observation();
        if (read == null || read.outcome() == null) return Decision.unknown("MISSING_CALL_EVIDENCE");
        switch (read.outcome()) {
            case EXCLUDED: return Decision.excluded(read.reason());
            case FAILED_INVALID, DELIVERED_INVALID: return Decision.unknown(read.reason());
            case DELIVERED_UNMAPPED: return Decision.excluded("COMMAND_OUTSIDE_DECLARED_SCOPE");
            case FAILED:
                return index.forwardAction(read.reader()) == null ? Decision.unknown("READER_ACTION_UNPROVEN")
                        : Decision.negative("FAILED_CALL_WITHOUT_DELIVERY");
            case DELIVERED: break;
        }
        if (!validContract(read.contract()) || contracts.stream().filter(read.contract()::equals).count() != 1)
            return Decision.unknown("UNDECLARED_OR_AMBIGUOUS_ADAPTER_CONTRACT");
        if (!Objects.equals(read.commandType(), read.contract().commandType())
                || !Objects.equals(read.responseType(), read.contract().responseType()))
            return Decision.unknown("RETURNED_CONTRACT_TYPE_MISMATCH");
        if (read.identity() == null || read.identity().aggregateId() == null || read.version() == null
                || !Objects.equals(read.identity().aggregateType(), read.contract().aggregateType()))
            return Decision.unknown("RETURNED_REVISION_IDENTITY_UNAVAILABLE");
        Action readerAction = index.forwardAction(read.reader());
        if (readerAction == null) return Decision.unknown("READER_ACTION_UNPROVEN");
        List<Write> producers = index.byRevision.getOrDefault(new RevisionKey(read.identity(), read.version()), List.of());
        if (producers.isEmpty()) {
            List<Revision> originals = index.baselineByIdentity.getOrDefault(read.identity(), List.of());
            if (originals.size() == 1 && Objects.equals(originals.getFirst().version(), read.version())
                    && Objects.equals(originals.getFirst().runtimeType(), read.contract().runtimeType()))
                return Decision.negative("REVISION_PREEXISTS_MEASUREMENT");
            return Decision.unknown("RETURNED_REVISION_UNATTRIBUTED");
        }
        if (producers.size() != 1) return Decision.unknown("AMBIGUOUS_REVISION_PRODUCER");
        Write creation = producers.getFirst();
        Revision revision = creation.revision();
        if (!Objects.equals(revision.runtimeType(), read.contract().runtimeType()))
            return Decision.unknown("PERSISTENT_RUNTIME_TYPE_COLLISION");
        Action producerAction = index.forwardAction(creation.writer());
        if (producerAction == null) return Decision.unknown("PRODUCER_ACTION_UNPROVEN");
        if (Objects.equals(creation.writer().sagaInstanceId(), read.reader().sagaInstanceId()))
            return Decision.negative("SAME_SAGA_READER");
        if (creation.order() >= call.order() || producerAction.actualPosition() >= readerAction.actualPosition())
            return Decision.unknown("CREATION_DELIVERY_ORDER_UNPROVEN");
        if (!revision.frameworkMetadataAvailable()) return Decision.unknown("CREATION_PREDECESSOR_UNAVAILABLE");
        if (revision.predecessorIdentity() != null || revision.predecessorVersion() != null
                || !"ACTIVE".equals(revision.lifecycleState())) return Decision.unknown("NON_CREATION_EFFECT_OUT_OF_SCOPE");
        if (!baselineCovered) return Decision.unknown("BASELINE_ABSENCE_UNCOVERED");
        List<Write> objectWrites = index.byIdentity.getOrDefault(revision.identity(), List.of());
        if (!index.baselineByIdentity.getOrDefault(revision.identity(), List.of()).isEmpty()
                || objectWrites.stream().anyMatch(write -> write.order() < creation.order()))
            return Decision.unknown("PRIOR_ABSENCE_NOT_ESTABLISHED");
        if (index.committedBySaga.getOrDefault(creation.writer().sagaInstanceId(), List.of()).stream()
                .anyMatch(action -> action.actualPosition() < readerAction.actualPosition()))
            return Decision.negative("PRODUCER_COMPLETED_BEFORE_DELIVERY");

        List<Write> deleted = objectWrites.stream().filter(write -> write.revision() != null
                && "DELETED".equals(write.revision().lifecycleState())).toList();
        if (deleted.stream().anyMatch(write -> write.order() < call.order() && directSuccessor(revision, write.revision())))
            return Decision.negative("DELETION_PRECEDES_DELIVERY");
        List<Write> laterDeletions = deleted.stream().filter(write -> write.order() > call.order()).toList();
        if (laterDeletions.isEmpty()) {
            if (lostWrites(gaps, Long.MAX_VALUE)) return Decision.unknown("WRITE_COVERAGE_INCOMPLETE");
            return completeExecution ? Decision.negative("NO_SUBSEQUENT_COMPENSATING_DELETION")
                    : Decision.unknown("COMPENSATION_HORIZON_INCOMPLETE");
        }
        List<Write> successors = laterDeletions.stream().filter(write -> directSuccessor(revision, write.revision())).toList();
        if (successors.size() != 1) return Decision.unknown(successors.isEmpty()
                ? "DIRECT_DELETION_PREDECESSOR_UNPROVEN" : "AMBIGUOUS_COMPENSATING_DELETION");
        Write deletion = successors.getFirst();
        if (lostWrites(gaps, deletion.order())) return Decision.unknown("WRITE_COVERAGE_INCOMPLETE");
        if (objectWrites.stream().anyMatch(write -> write.order() > creation.order() && write.order() < deletion.order()))
            return Decision.unknown("INTERMEDIATE_REVISION_OUT_OF_SCOPE");
        Action ordinaryDeletion = index.forwardAction(deletion.writer());
        if (completeExecution && ordinaryDeletion != null && ordinaryDeletion.actualPosition() > readerAction.actualPosition()
                && index.committedBySaga.getOrDefault(creation.writer().sagaInstanceId(), List.of()).stream()
                .anyMatch(action -> action.actualPosition() > readerAction.actualPosition()
                        && action.actualPosition() < ordinaryDeletion.actualPosition())) {
            return Decision.negative("ORDINARY_DELETION_AFTER_PRODUCER_SUCCESS");
        }
        if (!index.owned(deletion.writer()) || !Objects.equals(creation.writer().sagaInstanceId(), deletion.writer().sagaInstanceId())
                || !"RECOVERY".equals(deletion.writer().phase()))
            return Decision.unknown("DELETION_COMPENSATION_AUTHOR_UNPROVEN");
        Action recovery = index.action(deletion.writer());
        String recoveryGap = index.recoveryGap(recovery, producerAction, readerAction);
        if (recoveryGap != null) return Decision.unknown(recoveryGap);
        return new Decision("OBSERVED", "READ_OF_CREATION_SUBSEQUENTLY_COMPENSATED", creation, deletion);
    }

    private boolean directSuccessor(Revision creation, Revision deleted) {
        return deleted.frameworkMetadataAvailable()
                && Objects.equals(creation.identity(), deleted.predecessorIdentity())
                && Objects.equals(creation.version(), deleted.predecessorVersion())
                && Objects.equals(creation.runtimeType(), deleted.runtimeType())
                && deleted.version() != null && deleted.version() > creation.version();
    }

    private boolean lostWrites(List<Gap> gaps, long beforeOrder) {
        return gaps.stream().anyMatch(gap -> gap.afterOrder() < beforeOrder
                && Set.of("WRITE", "ATTEMPT", "OBSERVER_CALLBACK", "OBSERVER_ENABLEMENT").contains(gap.stage()));
    }

    private boolean validContract(ReadResponseEvidence.Contract contract) {
        return contract != null && !blank(contract.id()) && !blank(contract.version()) && !blank(contract.commandType())
                && !blank(contract.responseType()) && !blank(contract.aggregateType()) && !blank(contract.runtimeType());
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private record RevisionKey(ImpactEvidence.AggregateIdentity identity, Long version) { }
    private record Decision(String verdict, String reason, Write creation, Write deletion) {
        static Decision unknown(String reason) { return new Decision("UNKNOWN", reason, null, null); }
        static Decision negative(String reason) { return new Decision("NOT_OBSERVED", reason, null, null); }
        static Decision excluded(String reason) { return new Decision("EXCLUDED", reason, null, null); }
    }

    private static final class Index {
        private final String attemptId;
        private final String workloadId;
        private final Map<RevisionKey, List<Write>> byRevision;
        private final Map<ImpactEvidence.AggregateIdentity, List<Write>> byIdentity;
        private final Map<ImpactEvidence.AggregateIdentity, List<Revision>> baselineByIdentity;
        private final Map<String, List<Action>> actions;
        private final Map<String, List<Action>> committedBySaga;
        private final Map<String, List<Occurrence>> occurrences;
        private final Map<String, List<Checkpoint>> checkpoints;
        private final List<Action> actual;
        private final long lastOrder;

        Index(String attemptId, String workloadId, SourceContract source, List<Revision> baseline,
              List<Write> writes, List<Action> actual) {
            this.attemptId = attemptId;
            this.workloadId = workloadId;
            this.actual = actual;
            List<Write> usable = writes.stream().filter(write -> write.revision() != null
                    && write.revision().identity() != null).sorted(Comparator.comparingLong(Write::order)).toList();
            byRevision = group(usable, write -> new RevisionKey(write.revision().identity(), write.revision().version()));
            byIdentity = group(usable, write -> write.revision().identity());
            baselineByIdentity = group(baseline.stream().filter(value -> value != null && value.identity() != null).toList(), Revision::identity);
            actions = group(actual.stream().filter(value -> value.id() != null).toList(), Action::id);
            committedBySaga = group(actual.stream().filter(value -> "SUCCEEDED".equals(value.commitOutcome())
                    && value.sagaInstanceId() != null).toList(), Action::sagaInstanceId);
            occurrences = group(source.occurrences().stream().filter(value -> value.id() != null).toList(), Occurrence::id);
            checkpoints = group(source.checkpoints().stream().filter(value -> value.id() != null).toList(), Checkpoint::id);
            lastOrder = writes.stream().mapToLong(Write::order).max().orElse(0);
        }

        boolean owned(ImpactEvidence.Writer writer) {
            return writer != null && "SAGA".equals(writer.kind()) && Objects.equals(attemptId, writer.executionAttemptId())
                    && Objects.equals(workloadId, writer.workloadPlanId()) && !blank(writer.sagaInstanceId())
                    && !blank(writer.actionId()) && !blank(writer.functionalityName()) && !blank(writer.stepName());
        }

        Action action(ImpactEvidence.Writer writer) {
            if (!owned(writer)) return null;
            List<Action> matches = actions.getOrDefault(writer.actionId(), List.of());
            if (matches.size() != 1) return null;
            Action action = matches.getFirst();
            return Objects.equals(writer.sagaInstanceId(), action.sagaInstanceId())
                    && Objects.equals(writer.functionalityName(), action.functionalityName())
                    && Objects.equals(writer.stepName(), action.runtimeStepName()) ? action : null;
        }

        Action forwardAction(ImpactEvidence.Writer writer) {
            Action action = action(writer);
            if (action == null || !"FORWARD".equals(writer.phase()) || !"FORWARD".equals(action.kind())
                    || Set.of("ASSIGNED_FAULT", "NOT_REACHED", "SKIPPED").contains(String.valueOf(action.status()))) return null;
            List<Occurrence> matches = occurrences.getOrDefault(action.sourceScheduledStepId(), List.of());
            if (matches.size() != 1 || !sameSource(action, matches.getFirst())
                    || !Objects.equals(action.runtimeOccurrenceId(), matches.getFirst().id())) return null;
            return action;
        }

        String recoveryGap(Action recovery, Action producer, Action reader) {
            if (recovery == null || !"COMPENSATION".equals(recovery.kind())
                    || recovery.actualPosition() <= reader.actualPosition()) return "RECOVERY_ACTION_UNPROVEN";
            List<Checkpoint> matches = checkpoints.getOrDefault(recovery.checkpointId(), List.of());
            if (matches.size() != 1) return "RECOVERY_CHECKPOINT_UNPROVEN";
            Checkpoint checkpoint = matches.getFirst();
            if (!"EXPLICIT_COMPENSATION".equals(checkpoint.evidenceClass())
                    || !Objects.equals(recovery.compensationEvidenceClass(), checkpoint.evidenceClass())
                    || !Objects.equals(checkpoint.sagaInstanceId(), producer.sagaInstanceId())
                    || !Objects.equals(checkpoint.sourceScheduledStepId(), producer.sourceScheduledStepId())
                    || !Objects.equals(checkpoint.stepId(), producer.sourceStepId())
                    || !Objects.equals(checkpoint.runtimeStepName(), producer.runtimeStepName())
                    || !Objects.equals(recovery.sourceScheduledStepId(), checkpoint.sourceScheduledStepId())
                    || !Objects.equals(recovery.sourceStepId(), checkpoint.stepId())
                    || !Objects.equals(recovery.runtimeStepName(), checkpoint.runtimeStepName())
                    || !Objects.equals(recovery.runtimeOccurrenceId(), checkpoint.occurrenceId()))
                return "COMPENSATION_PRODUCING_OCCURRENCE_MISMATCH";
            if (recovery.plannedPosition() == null) {
                // The executor fallback chooses a source by name. Do not inherit its choice when ambiguous.
                long eligible = actual.stream().filter(action -> "FORWARD".equals(action.kind())
                        && Objects.equals(action.sagaInstanceId(), recovery.sagaInstanceId())
                        && Objects.equals(action.runtimeStepName(), recovery.runtimeStepName())
                        && action.actualPosition() < recovery.actualPosition()).count();
                if (eligible != 1) return "AMBIGUOUS_RUNTIME_RECOVERY_SOURCE";
            }
            List<Recovery> explicit = recovery.recovery().stream()
                    .filter(result -> "EXPLICIT_COMPENSATION".equals(result.kind())).toList();
            if (explicit.size() != 1 || !"SUCCEEDED".equals(explicit.getFirst().status()))
                return "EXPLICIT_COMPENSATION_SUCCESS_UNPROVEN";
            return null;
        }

        private boolean sameSource(Action action, Occurrence source) {
            return Objects.equals(action.sagaInstanceId(), source.sagaInstanceId())
                    && Objects.equals(action.sourceStepId(), source.stepId())
                    && Objects.equals(action.runtimeStepName(), source.runtimeStepName());
        }

        private static <T, K> Map<K, List<T>> group(List<T> values, Function<T, K> key) {
            return values.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
        }
    }
}
