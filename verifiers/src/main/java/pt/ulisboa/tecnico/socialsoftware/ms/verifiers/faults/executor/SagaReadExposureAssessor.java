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
            if (decision.produced != null) {
                Write produced = decision.produced;
                Write recoveryWrite = decision.recovery;
                String reader = call.observation().reader().sagaInstanceId();
                findingId = "exposure:" + UUID.nameUUIDFromBytes((attemptId + "\n" + decision.category + "\n"
                        + produced.id() + "\n" + produced.revision().version() + "\n" + reader)
                        .getBytes(StandardCharsets.UTF_8));
                Finding previous = findings.get(findingId);
                List<String> deliveries = new ArrayList<>(previous == null ? List.of() : previous.deliveryIds());
                deliveries.add(call.id());
                Action recovery = index.action(recoveryWrite.writer());
                boolean creation = "CREATION".equals(decision.category);
                findings.put(findingId, new Finding(findingId, "OBSERVED", decision.category,
                        produced.revision().identity(), produced.revision().runtimeType(),
                        produced.revision().version(), recoveryWrite.revision().version(),
                        produced.writer().sagaInstanceId(), reader, produced.id(), recoveryWrite.id(),
                        recovery.sourceScheduledStepId(), recovery.checkpointId(), decision.restoredAttributes,
                        decision.notRestoredAttributes, creation ? produced.revision().version() : null,
                        creation ? recoveryWrite.revision().version() : null, creation ? produced.id() : null,
                        creation ? recoveryWrite.id() : null, deliveries));
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
        Write produced = producers.getFirst();
        Revision revision = produced.revision();
        if (!Objects.equals(revision.runtimeType(), read.contract().runtimeType()))
            return Decision.unknown("PERSISTENT_RUNTIME_TYPE_COLLISION");
        Action producerAction = index.forwardAction(produced.writer());
        if (producerAction == null) {
            Action producingAction = index.action(produced.writer());
            if (producingAction != null && "RECOVERY".equals(produced.writer().phase())
                    && "COMPENSATION".equals(producingAction.kind()))
                return Decision.negative("REVISION_PRODUCED_BY_RECOVERY");
            return Decision.unknown("PRODUCER_ACTION_UNPROVEN");
        }
        if (Objects.equals(produced.writer().sagaInstanceId(), read.reader().sagaInstanceId()))
            return Decision.negative("SAME_SAGA_READER");
        if (produced.order() >= call.order() || producerAction.actualPosition() >= readerAction.actualPosition())
            return Decision.unknown("CREATION_DELIVERY_ORDER_UNPROVEN");
        if (!revision.frameworkMetadataAvailable()) return Decision.unknown("CREATION_PREDECESSOR_UNAVAILABLE");
        if (!"ACTIVE".equals(revision.lifecycleState())) return Decision.unknown("NON_ACTIVE_FORWARD_EFFECT_OUT_OF_SCOPE");
        if (index.committedBySaga.getOrDefault(produced.writer().sagaInstanceId(), List.of()).stream()
                .anyMatch(action -> action.actualPosition() < readerAction.actualPosition()))
            return Decision.negative("PRODUCER_COMPLETED_BEFORE_DELIVERY");
        if (revision.predecessorIdentity() == null && revision.predecessorVersion() == null)
            return evaluateCreation(call, index, baselineCovered, completeExecution, gaps, produced,
                    revision, producerAction, readerAction);
        if (revision.predecessorIdentity() == null || revision.predecessorVersion() == null)
            return Decision.unknown("UPDATE_PREDECESSOR_UNAVAILABLE");
        return evaluateUpdate(call, index, completeExecution, gaps, produced, revision, producerAction, readerAction);
    }

    private Decision evaluateCreation(Call call, Index index, boolean baselineCovered, boolean completeExecution,
                                      List<Gap> gaps, Write creation, Revision revision,
                                      Action producerAction, Action readerAction) {
        if (!baselineCovered) return Decision.unknown("BASELINE_ABSENCE_UNCOVERED");
        List<Write> objectWrites = index.byIdentity.getOrDefault(revision.identity(), List.of());
        if (!index.baselineByIdentity.getOrDefault(revision.identity(), List.of()).isEmpty()
                || objectWrites.stream().anyMatch(write -> write.order() < creation.order()))
            return Decision.unknown("PRIOR_ABSENCE_NOT_ESTABLISHED");
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
        return Decision.observed("READ_OF_CREATION_SUBSEQUENTLY_COMPENSATED", "CREATION", creation, deletion,
                List.of(), List.of());
    }

    private Decision evaluateUpdate(Call call, Index index, boolean completeExecution, List<Gap> gaps,
                                    Write forward, Revision revision, Action producerAction, Action readerAction) {
        if (!Objects.equals(revision.identity(), revision.predecessorIdentity()))
            return Decision.unknown("UPDATE_PREDECESSOR_IDENTITY_MISMATCH");
        List<Revision> predecessors = index.predecessors(forward);
        if (predecessors.size() != 1) return Decision.unknown(predecessors.isEmpty()
                ? "UPDATE_PREDECESSOR_UNAVAILABLE" : "AMBIGUOUS_UPDATE_PREDECESSOR");
        Revision predecessor = predecessors.getFirst();
        if (!Objects.equals(predecessor.runtimeType(), revision.runtimeType()))
            return Decision.unknown("UPDATE_PREDECESSOR_RUNTIME_TYPE_MISMATCH");
        if (predecessor.applicationAttributeFingerprints().isEmpty()
                || revision.applicationAttributeFingerprints().isEmpty()
                || !predecessor.applicationAttributeFingerprints().keySet()
                .equals(revision.applicationAttributeFingerprints().keySet())
                || gaps.stream().anyMatch(gap -> "BASELINE".equals(gap.stage())))
            return Decision.unknown("APPLICATION_ATTRIBUTE_PROJECTION_INCOMPLETE");
        List<String> changed = changedAttributes(predecessor, revision);
        if (changed.isEmpty()) return Decision.negative("NO_APPLICATION_ATTRIBUTE_CHANGE");

        List<Write> objectWrites = index.byIdentity.getOrDefault(revision.identity(), List.of());
        List<Write> writesBeforeRead = objectWrites.stream().filter(write -> write.order() > forward.order()
                && write.order() < call.order()).toList();
        List<Write> recoveriesBeforeRead = writesBeforeRead.stream().filter(write -> directSuccessor(revision, write.revision())
                && "ACTIVE".equals(write.revision().lifecycleState()) && write.writer() != null
                && "RECOVERY".equals(write.writer().phase())
                && Objects.equals(forward.writer().sagaInstanceId(), write.writer().sagaInstanceId())).toList();
        if (!recoveriesBeforeRead.isEmpty()) return Decision.negative("RECOVERY_PRECEDES_DELIVERY");
        if (!writesBeforeRead.isEmpty()) return Decision.unknown("INTERVENING_WRITER");
        List<Write> later = objectWrites.stream().filter(write -> write.order() > call.order()
                && "ACTIVE".equals(write.revision().lifecycleState())
                && write.writer() != null && "RECOVERY".equals(write.writer().phase())
                && Objects.equals(forward.writer().sagaInstanceId(), write.writer().sagaInstanceId())).toList();
        if (later.isEmpty()) {
            if (lostWrites(gaps, Long.MAX_VALUE)) return Decision.unknown("WRITE_COVERAGE_INCOMPLETE");
            if (objectWrites.stream().anyMatch(write -> write.order() > call.order()))
                return Decision.unknown("INTERVENING_WRITER");
            return completeExecution ? Decision.negative("NO_SUBSEQUENT_COMPENSATING_UPDATE")
                    : Decision.unknown("COMPENSATION_HORIZON_INCOMPLETE");
        }
        if (later.size() != 1) return Decision.unknown("AMBIGUOUS_COMPENSATING_UPDATE");
        Write recovery = later.getFirst();
        if (lostWrites(gaps, recovery.order())) return Decision.unknown("WRITE_COVERAGE_INCOMPLETE");
        if (objectWrites.stream().anyMatch(write -> write.order() > forward.order() && write.order() < recovery.order()))
            return Decision.unknown("INTERVENING_WRITER");
        if (!directSuccessor(revision, recovery.revision()))
            return Decision.unknown("DIRECT_UPDATE_RECOVERY_PREDECESSOR_UNPROVEN");
        if (recovery.revision().applicationAttributeFingerprints().isEmpty()
                || !predecessor.applicationAttributeFingerprints().keySet()
                .equals(recovery.revision().applicationAttributeFingerprints().keySet()))
            return Decision.unknown("APPLICATION_ATTRIBUTE_PROJECTION_INCOMPLETE");
        if (!index.owned(recovery.writer())
                || !Objects.equals(forward.writer().sagaInstanceId(), recovery.writer().sagaInstanceId())
                || !"RECOVERY".equals(recovery.writer().phase()))
            return Decision.unknown("UPDATE_COMPENSATION_AUTHOR_UNPROVEN");
        Action recoveryAction = index.action(recovery.writer());
        String recoveryGap = index.recoveryGap(recoveryAction, producerAction, readerAction);
        if (recoveryGap != null) return Decision.unknown(recoveryGap);

        List<String> restored = changed.stream().filter(attribute -> Objects.equals(
                predecessor.applicationAttributeFingerprints().get(attribute),
                recovery.revision().applicationAttributeFingerprints().get(attribute))).toList();
        List<String> notRestored = changed.stream().filter(attribute -> !restored.contains(attribute)).toList();
        if (restored.isEmpty()) return Decision.negative("NO_CHANGED_ATTRIBUTE_RESTORED");
        return Decision.observed(notRestored.isEmpty() ? "READ_OF_UPDATE_SUBSEQUENTLY_COMPENSATED"
                : "READ_OF_UPDATE_SUBSEQUENTLY_PARTIALLY_COMPENSATED", "UPDATE", forward, recovery,
                restored, notRestored);
    }

    private List<String> changedAttributes(Revision predecessor, Revision forward) {
        Set<String> names = new java.util.TreeSet<>(predecessor.applicationAttributeFingerprints().keySet());
        names.addAll(forward.applicationAttributeFingerprints().keySet());
        return names.stream().filter(name -> !Objects.equals(predecessor.applicationAttributeFingerprints().get(name),
                forward.applicationAttributeFingerprints().get(name))).toList();
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
    private record Decision(String verdict, String reason, String category, Write produced, Write recovery,
                            List<String> restoredAttributes, List<String> notRestoredAttributes) {
        static Decision observed(String reason, String category, Write produced, Write recovery,
                                 List<String> restored, List<String> notRestored) {
            return new Decision("OBSERVED", reason, category, produced, recovery, restored, notRestored);
        }
        static Decision unknown(String reason) { return new Decision("UNKNOWN", reason, null, null, null, List.of(), List.of()); }
        static Decision negative(String reason) { return new Decision("NOT_OBSERVED", reason, null, null, null, List.of(), List.of()); }
        static Decision excluded(String reason) { return new Decision("EXCLUDED", reason, null, null, null, List.of(), List.of()); }
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

        List<Revision> predecessors(Write forward) {
            Revision revision = forward.revision();
            RevisionKey key = new RevisionKey(revision.predecessorIdentity(), revision.predecessorVersion());
            List<Revision> result = new ArrayList<>();
            baselineByIdentity.getOrDefault(revision.predecessorIdentity(), List.of()).stream()
                    .filter(value -> Objects.equals(value.version(), revision.predecessorVersion()))
                    .forEach(result::add);
            byRevision.getOrDefault(key, List.of()).stream().filter(write -> write.order() < forward.order())
                    .map(Write::revision).forEach(result::add);
            return result;
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
