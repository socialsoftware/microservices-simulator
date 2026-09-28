package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Pure, deterministic ImpactV2 checks over one completed attempt's collected evidence. */
class ImpactV2Assessor {
    static final String DELETED_DEPENDENCY = "DELETED_DEPENDENCY";
    static final String FAILED_OPERATION_RESIDUAL = "FAILED_OPERATION_RESIDUAL";
    static final String UNRESOLVED_DELIVERED_EVENT = "UNRESOLVED_DELIVERED_EVENT";

    private static final Comparator<ImpactEvidence.AggregateIdentity> IDENTITY_ORDER =
            Comparator.comparing(ImpactEvidence.AggregateIdentity::aggregateType,
                            Comparator.nullsFirst(String::compareTo))
                    .thenComparing(ImpactEvidence.AggregateIdentity::aggregateId,
                            Comparator.nullsFirst(Integer::compareTo));
    private static final Comparator<ImpactV2EvidenceReport.Candidate> CANDIDATE_ORDER =
            Comparator.comparing(ImpactV2EvidenceReport.Candidate::aggregate,
                            Comparator.nullsFirst(IDENTITY_ORDER))
                    .thenComparing(ImpactV2EvidenceReport.Candidate::eventId,
                            Comparator.nullsFirst(Integer::compareTo));

    ImpactV2EvidenceReport assess(ScenarioExecutionReport execution,
                                  String attemptId,
                                  String workloadPlanId,
                                  String faultScenarioId,
                                  String collectionStatus,
                                  String collectionReason,
                                  List<ImpactEvidence.AggregateSnapshot> baseline,
                                  List<ImpactEvidence.AggregateSnapshot> finalState,
                                  List<ImpactEvidence.CommittedWrite> writes,
                                  List<ImpactEvidence.EventDelivery> deliveries,
                                  List<ImpactEvidence.CoverageGap> gaps) {
        String horizon = valid(execution) ? "FINAL_SCHEDULED_ACTION" : "INCOMPLETE_EXECUTION";
        if (!valid(execution)) {
            return report(execution, attemptId, workloadPlanId, faultScenarioId, "INVALID",
                    "EXECUTION_NOT_COMPLETE", collectionStatus, collectionReason, horizon, null, null,
                    terminalCategories("INVALID"), baseline, finalState, writes, deliveries, gaps);
        }
        if ("UNAVAILABLE".equals(collectionStatus)) {
            return report(execution, attemptId, workloadPlanId, faultScenarioId, "UNAVAILABLE",
                    collectionReason, collectionStatus, collectionReason, horizon, null, null,
                    terminalCategories("UNAVAILABLE"), baseline, finalState, writes, deliveries, gaps);
        }

        EvidenceIndex evidence = new EvidenceIndex(baseline, finalState, writes);
        ImpactV2EvidenceReport.CategoryResult deleted = deletedDependencies(evidence, gaps);
        ImpactV2EvidenceReport.CategoryResult residual = failedOperationResiduals(
                execution, attemptId, workloadPlanId, evidence, deliveries, gaps);
        ImpactV2EvidenceReport.CategoryResult event = unresolvedDeliveredEvents(execution, deliveries, gaps);
        List<ImpactV2EvidenceReport.CategoryResult> categories = List.of(deleted, residual, event);
        Set<ImpactEvidence.AggregateIdentity> affected = new TreeSet<>(IDENTITY_ORDER);
        categories.forEach(category -> category.findings().forEach(finding -> {
            if (finding.affectedObject() != null) affected.add(finding.affectedObject());
        }));
        boolean complete = categories.stream().allMatch(category -> "COMPLETE".equals(category.coverageStatus()));
        return report(execution, attemptId, workloadPlanId, faultScenarioId,
                complete ? "COMPLETE" : "PARTIAL", complete ? null : "CATEGORY_COVERAGE_GAPS",
                collectionStatus, collectionReason, horizon, complete ? affected.size() : null, affected.size(),
                categories, baseline, finalState, writes, deliveries, gaps);
    }

    private ImpactV2EvidenceReport.CategoryResult deletedDependencies(
            EvidenceIndex evidence, List<ImpactEvidence.CoverageGap> gaps) {
        List<ImpactV2EvidenceReport.UnknownReason> unknowns = gapUnknowns(DELETED_DEPENDENCY, gaps,
                Set.of("SNAPSHOT", "ATTEMPT", "OBSERVER_CALLBACK", "OBSERVER_ENABLEMENT"));
        Set<ImpactV2EvidenceReport.Candidate> candidates = new TreeSet<>(CANDIDATE_ORDER);
        List<ImpactV2EvidenceReport.Finding> findings = new ArrayList<>();
        for (ImpactEvidence.AggregateSnapshot source : evidence.finalSnapshots()) {
            if (source.identity() == null || !"ACTIVE".equals(source.lifecycleState())) continue;
            if (source.dependencies() == null) continue;
            for (ImpactEvidence.Dependency dependency : source.dependencies()) {
                candidates.add(new ImpactV2EvidenceReport.Candidate(source.identity(), null));
                ImpactEvidence.AggregateIdentity target = dependency.target();
                if (target == null) {
                    unknowns.add(unknown(DELETED_DEPENDENCY, "DEPENDENCY_TARGET_IDENTITY_UNAVAILABLE",
                            String.valueOf(dependency.declaredTargetAggregateId()), source.identity(), null));
                    continue;
                }
                ImpactEvidence.AggregateSnapshot targetFinal = evidence.finalById().get(target);
                if (targetFinal == null) {
                    unknowns.add(unknown(DELETED_DEPENDENCY, "DEPENDENCY_TARGET_FINAL_STATE_UNAVAILABLE",
                            target.toString(), source.identity(), null));
                    continue;
                }
                if (!"DELETED".equals(targetFinal.lifecycleState())) continue;
                ImpactEvidence.AggregateSnapshot targetBaseline = evidence.baselineById().get(target);
                if (targetBaseline != null && "DELETED".equals(targetBaseline.lifecycleState())) continue;
                List<ImpactEvidence.CommittedWrite> targetWrites = evidence.writesById().getOrDefault(target, List.of());
                ImpactEvidence.CommittedWrite deletedWrite = targetWrites.stream()
                        .filter(write -> "DELETED".equals(write.aggregate().lifecycleState()))
                        .findFirst().orElse(null);
                boolean observedNonDeleted = targetBaseline != null
                        && !"DELETED".equals(targetBaseline.lifecycleState());
                if (!observedNonDeleted && deletedWrite != null) {
                    observedNonDeleted = targetWrites.stream()
                            .anyMatch(write -> write.sequence() < deletedWrite.sequence()
                                    && !"DELETED".equals(write.aggregate().lifecycleState()));
                }
                if (deletedWrite == null || !observedNonDeleted) {
                    unknowns.add(unknown(DELETED_DEPENDENCY, "DELETION_TRANSITION_UNOBSERVED",
                            target.toString(), source.identity(), null));
                    continue;
                }
                findings.add(new ImpactV2EvidenceReport.Finding(DELETED_DEPENDENCY,
                        "ACTIVE_SOURCE_DEPENDS_ON_TARGET_DELETED_DURING_ATTEMPT", source.identity(), target, null,
                        actionIds(targetWrites), versions(targetBaseline, targetFinal, targetWrites)));
            }
        }
        return category(DELETED_DEPENDENCY, candidates, findings, unknowns);
    }

    private ImpactV2EvidenceReport.CategoryResult failedOperationResiduals(
            ScenarioExecutionReport execution,
            String attemptId,
            String workloadPlanId,
            EvidenceIndex evidence,
            List<ImpactEvidence.EventDelivery> deliveries,
            List<ImpactEvidence.CoverageGap> gaps) {
        List<ImpactV2EvidenceReport.UnknownReason> unknowns = gapUnknowns(FAILED_OPERATION_RESIDUAL, gaps,
                Set.of("SNAPSHOT", "WRITE", "ATTEMPT", "OBSERVER_CALLBACK", "OBSERVER_ENABLEMENT"));
        boolean comparisonEvidenceIncomplete = gaps.stream().anyMatch(this::invalidatesPersistentComparison);
        Set<String> failed = execution.actualActions().stream()
                .filter(action -> Set.of("ASSIGNED_FAULT", "FAILED", "COMMIT_FAILED").contains(action.status()))
                .map(ScenarioExecutionReport.ActionOutcome::sagaInstanceId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        Set<String> recovered = execution.participants().stream()
                .filter(participant -> "COMPENSATED".equals(participant.finalState()))
                .map(ScenarioExecutionReport.Participant::sagaInstanceId)
                .filter(id -> execution.lifecycleEvents().stream().anyMatch(event -> id.equals(event.sagaInstanceId())
                        && "COMPENSATED".equals(event.type()) && "SUCCEEDED".equals(event.outcome())))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

        Set<ImpactEvidence.AggregateIdentity> candidateIds = new TreeSet<>(IDENTITY_ORDER);
        for (ImpactEvidence.CommittedWrite write : evidence.writes()) {
            ImpactEvidence.Writer writer = write.writer();
            if (writer == null || "UNKNOWN".equals(writer.kind())
                    || (writer.sagaInstanceId() != null && failed.contains(writer.sagaInstanceId()))) {
                if (write.aggregate() != null && write.aggregate().identity() != null) {
                    candidateIds.add(write.aggregate().identity());
                }
            }
        }
        Set<ImpactV2EvidenceReport.Candidate> candidates = candidateIds.stream()
                .map(identity -> new ImpactV2EvidenceReport.Candidate(identity, null))
                .collect(java.util.stream.Collectors.toCollection(() -> new TreeSet<>(CANDIDATE_ORDER)));
        List<ImpactV2EvidenceReport.Finding> findings = new ArrayList<>();
        for (ImpactEvidence.AggregateIdentity identity : candidateIds) {
            List<ImpactEvidence.CommittedWrite> objectWrites = evidence.writesById().getOrDefault(identity, List.of());
            Set<String> failedWriters = objectWrites.stream().map(ImpactEvidence.CommittedWrite::writer)
                    .filter(Objects::nonNull).filter(writer -> "SAGA".equals(writer.kind())
                            && writer.sagaInstanceId() != null
                            && failed.contains(writer.sagaInstanceId()))
                    .map(ImpactEvidence.Writer::sagaInstanceId)
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            if (failedWriters.size() != 1) {
                unknowns.add(unknown(FAILED_OPERATION_RESIDUAL, "FAILED_WRITER_IDENTITY_UNAVAILABLE",
                        identity.toString(), identity, null));
                continue;
            }
            String failedSaga = failedWriters.iterator().next();
            if (!recovered.contains(failedSaga)) {
                unknowns.add(unknown(FAILED_OPERATION_RESIDUAL, "FAILED_SAGA_RECOVERY_INCOMPLETE",
                        failedSaga, identity, null));
                continue;
            }
            boolean soleWriter = objectWrites.stream().allMatch(write -> ownedFailedSagaWrite(
                    write.writer(), attemptId, workloadPlanId, failedSaga));
            if (!soleWriter) {
                if (!comparisonEvidenceIncomplete) {
                    assessExclusiveFields(execution, attemptId, workloadPlanId, failedSaga, identity,
                            evidence.baselineById().get(identity), evidence.finalById().get(identity),
                            objectWrites, deliveries, findings, unknowns);
                }
                continue;
            }
            if (comparisonEvidenceIncomplete) continue;
            ImpactEvidence.AggregateSnapshot finalSnapshot = evidence.finalById().get(identity);
            ImpactEvidence.CommittedWrite latestWrite = objectWrites.get(objectWrites.size() - 1);
            if (finalSnapshot == null || !samePersistentState(latestWrite.aggregate(), finalSnapshot)) {
                unknowns.add(unknown(FAILED_OPERATION_RESIDUAL, "FINAL_STATE_NOT_EXPLAINED_BY_TRACKED_WRITE",
                        identity.toString(), identity, null));
                continue;
            }
            ImpactEvidence.AggregateSnapshot initialSnapshot = evidence.baselineById().get(identity);
            // A new object logically removed during recovery is a storage remnant, not
            // a residual by itself. Active dependants are assessed independently above.
            if (initialSnapshot == null && "DELETED".equals(finalSnapshot.lifecycleState())
                    && "FORWARD".equals(objectWrites.get(0).writer().phase())
                    && "ACTIVE".equals(objectWrites.get(0).aggregate().lifecycleState())
                    && "RECOVERY".equals(latestWrite.writer().phase())
                    && objectWrites.stream()
                    .filter(write -> "DELETED".equals(write.aggregate().lifecycleState()))
                    .findFirst().map(write -> "RECOVERY".equals(write.writer().phase())).orElse(false)) {
                continue;
            }
            if (!samePersistentState(initialSnapshot, finalSnapshot)) {
                findings.add(new ImpactV2EvidenceReport.Finding(FAILED_OPERATION_RESIDUAL,
                        "FAILED_SAGA_LEFT_PERSISTENT_DIFFERENCE_AFTER_COMPLETED_RECOVERY", identity, null, null,
                        actionIds(objectWrites), versions(initialSnapshot, finalSnapshot, objectWrites)));
            }
        }
        return category(FAILED_OPERATION_RESIDUAL, candidates, findings, unknowns);
    }

    /** Attribute observed changes, not dependencies between fields or application intent. */
    private void assessExclusiveFields(
            ScenarioExecutionReport execution, String attemptId, String workloadPlanId, String failedSaga,
            ImpactEvidence.AggregateIdentity identity, ImpactEvidence.AggregateSnapshot baseline,
            ImpactEvidence.AggregateSnapshot finalState, List<ImpactEvidence.CommittedWrite> writes,
            List<ImpactEvidence.EventDelivery> deliveries,
            List<ImpactV2EvidenceReport.Finding> findings, List<ImpactV2EvidenceReport.UnknownReason> unknowns) {
        String problem = null;
        Set<String> committed = execution.participants().stream()
                .filter(participant -> "COMMITTED".equals(participant.finalState()))
                .map(ScenarioExecutionReport.Participant::sagaInstanceId).collect(java.util.stream.Collectors.toSet());
        for (ImpactEvidence.CommittedWrite write : writes) {
            ImpactEvidence.Writer writer = write.writer();
            boolean attributedSaga = writer != null && writer.sagaInstanceId() != null
                    && ownedFailedSagaWrite(writer, attemptId, workloadPlanId, writer.sagaInstanceId())
                    && (failedSaga.equals(writer.sagaInstanceId())
                    || (committed.contains(writer.sagaInstanceId()) && "FORWARD".equals(writer.phase())));
            boolean attributedEvent = provenSuccessfulEventWrite(
                    execution, attemptId, workloadPlanId, write, deliveries);
            if (!attributedSaga && !attributedEvent) {
                problem = "COMPETING_OR_UNKNOWN_WRITER";
                break;
            }
        }
        if (problem == null && baseline == null) problem = "RESIDUAL_BASELINE_UNAVAILABLE";
        ImpactEvidence.AggregateSnapshot latest = writes.get(writes.size() - 1).aggregate();
        if (problem == null && (finalState == null || !samePersistentState(latest, finalState)
                || !Objects.equals(latest.version(), finalState.version()))) {
            problem = "FINAL_STATE_NOT_EXPLAINED_BY_TRACKED_WRITE";
        }
        Set<String> failedFields = new TreeSet<>();
        Set<String> otherFields = new TreeSet<>();
        Map<String, String> stableListKeys = stableListKeys(baseline, finalState, writes);
        ImpactEvidence.AggregateSnapshot previous = baseline;
        long previousSequence = -1;
        if (problem == null) {
            for (ImpactEvidence.CommittedWrite write : writes) {
                ImpactEvidence.AggregateSnapshot next = write.aggregate();
                ImpactEvidence.FrameworkMetadata metadata = next.frameworkMetadata();
                if (previous.version() == null || next.version() == null || next.version() <= previous.version()
                        || metadata == null || !previous.identity().equals(metadata.predecessorIdentity())
                        || !previous.version().equals(metadata.predecessorVersion())
                        || write.sequence() <= previousSequence) {
                    problem = "RESIDUAL_VERSION_CHAIN_INCOMPLETE";
                    break;
                }
                Set<String> fields = changedFields(previous, next, stableListKeys);
                if (ownedFailedSagaWrite(write.writer(), attemptId, workloadPlanId, failedSaga)) {
                    failedFields.addAll(fields);
                } else {
                    otherFields.addAll(fields);
                }
                previous = next;
                previousSequence = write.sequence();
            }
        }
        if (problem != null) {
            unknowns.add(unknown(FAILED_OPERATION_RESIDUAL, problem, identity.toString(), identity, null));
            return;
        }
        Set<String> finalDifferences = changedFields(baseline, finalState, stableListKeys);
        Set<String> exclusiveResidual = new TreeSet<>(failedFields);
        exclusiveResidual.removeIf(field -> otherFields.stream().anyMatch(other -> overlappingPath(field, other))
                || !finalDifferences.contains(field));
        boolean overlap = failedFields.stream()
                .anyMatch(field -> otherFields.stream().anyMatch(other -> overlappingPath(field, other)));
        boolean provenKeyedResidual = exclusiveResidual.stream().anyMatch(field -> stableListKeys.keySet().stream()
                .anyMatch(name -> field.startsWith("/applicationData/" + escape(name) + "/")));
        if (overlap && !provenKeyedResidual) {
            unknowns.add(unknown(FAILED_OPERATION_RESIDUAL, "COMPETING_FIELD_CHANGE",
                    identity.toString(), identity, null));
            return;
        }
        if (!exclusiveResidual.isEmpty()) {
            findings.add(new ImpactV2EvidenceReport.Finding(FAILED_OPERATION_RESIDUAL,
                    "FAILED_SAGA_LEFT_EXCLUSIVE_FIELD_DIFFERENCE_AFTER_COMPLETED_RECOVERY", identity, null, null,
                    actionIds(writes), versions(baseline, finalState, writes), List.copyOf(exclusiveResidual)));
        }
    }

    private boolean provenSuccessfulEventWrite(
            ScenarioExecutionReport execution,
            String attemptId,
            String workloadPlanId,
            ImpactEvidence.CommittedWrite write,
            List<ImpactEvidence.EventDelivery> deliveries) {
        ImpactEvidence.Writer writer = write.writer();
        if (writer == null || !"EVENT_CONSUMER".equals(writer.kind()) || !"EVENT".equals(writer.phase())
                || !Objects.equals(attemptId, writer.executionAttemptId())
                || !Objects.equals(workloadPlanId, writer.workloadPlanId())
                || writer.actionId() == null || writer.eventId() == null || write.aggregate() == null) {
            return false;
        }
        List<ScenarioExecutionReport.ActionOutcome> actions = execution.actualActions().stream()
                .filter(action -> "EVENT_CONSEQUENCE".equals(action.kind()))
                .filter(action -> "COMPLETED".equals(action.status()) && "SUCCEEDED".equals(action.bodyOutcome()))
                .filter(action -> Objects.equals(writer.actionId(), action.actionId()))
                .filter(action -> Objects.equals(writer.sagaInstanceId(), action.sagaInstanceId()))
                .filter(action -> action.eventEvidence() != null
                        && Objects.equals(writer.eventId(), action.eventEvidence().eventId())
                        && Objects.equals(writer.functionalityName(), action.eventEvidence().eventHandlingClassFqn())
                        && Objects.equals(writer.stepName(), action.eventEvidence().eventHandlingMethodName()))
                .toList();
        if (actions.size() != 1) return false;
        ScenarioExecutionReport.ActionOutcome action = actions.getFirst();
        List<ImpactEvidence.EventDelivery> matches = deliveries.stream()
                .filter(delivery -> Objects.equals(writer, delivery.writer()))
                .filter(delivery -> exactDeliveryMatch(execution, action, delivery))
                .filter(delivery -> delivery.receiverAfter() != null
                        && Objects.equals(write.aggregate().version(), delivery.receiverAfter().version())
                        && samePersistentState(write.aggregate(), delivery.receiverAfter()))
                .toList();
        return matches.size() == 1;
    }

    private Set<String> changedFields(ImpactEvidence.AggregateSnapshot before,
                                      ImpactEvidence.AggregateSnapshot after) {
        return changedFields(before, after, Map.of());
    }

    private Set<String> changedFields(ImpactEvidence.AggregateSnapshot before,
                                      ImpactEvidence.AggregateSnapshot after,
                                      Map<String, String> stableListKeys) {
        Set<String> fields = new TreeSet<>();
        if (!Objects.equals(before.lifecycleState(), after.lifecycleState())) fields.add("/lifecycleState");
        Set<String> names = new TreeSet<>(before.applicationData().keySet());
        names.addAll(after.applicationData().keySet());
        for (String name : names) {
            Object oldValue = before.applicationData().get(name);
            Object newValue = after.applicationData().get(name);
            if (stableListKeys.containsKey(name) && oldValue instanceof List<?> oldList
                    && newValue instanceof List<?> newList) {
                changedKeyedList(fields, "/applicationData/" + escape(name),
                        oldList, newList, stableListKeys.get(name));
                continue;
            }
            if (before.applicationData().containsKey(name) != after.applicationData().containsKey(name)
                    || !Objects.equals(oldValue, newValue)) {
                fields.add("/applicationData/" + escape(name));
            }
        }
        return fields;
    }

    private Map<String, String> stableListKeys(ImpactEvidence.AggregateSnapshot baseline,
                                                ImpactEvidence.AggregateSnapshot finalState,
                                                List<ImpactEvidence.CommittedWrite> writes) {
        if (baseline == null || finalState == null) return Map.of();
        List<ImpactEvidence.AggregateSnapshot> snapshots = new ArrayList<>();
        snapshots.add(baseline);
        writes.forEach(write -> snapshots.add(write.aggregate()));
        snapshots.add(finalState);
        Map<String, String> result = new TreeMap<>();
        for (String name : baseline.applicationData().keySet()) {
            List<List<?>> lists = new ArrayList<>();
            for (ImpactEvidence.AggregateSnapshot snapshot : snapshots) {
                Object value = snapshot.applicationData().get(name);
                if (!(value instanceof List<?> list)) break;
                lists.add(list);
            }
            if (lists.size() != snapshots.size()) continue;
            Map<?, ?> first = lists.stream().flatMap(List::stream)
                    .filter(Map.class::isInstance).map(Map.class::cast).findFirst().orElse(null);
            if (first == null) continue;
            List<String> valid = first.keySet().stream().filter(String.class::isInstance)
                    .map(String.class::cast).filter(key -> key.endsWith("AggregateId"))
                    .filter(key -> lists.stream().allMatch(list -> uniqueListKey(list, key))).sorted().toList();
            if (valid.size() == 1) result.put(name, valid.getFirst());
        }
        return result;
    }

    private boolean uniqueListKey(List<?> list, String key) {
        Set<Object> identities = new java.util.HashSet<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map) || !map.containsKey(key)) return false;
            if (map.keySet().stream().anyMatch(name -> !(name instanceof String))) return false;
            Object value = map.get(key);
            if (!(value instanceof String || value instanceof Number) || !identities.add(value)) return false;
        }
        return true;
    }

    private void changedKeyedList(Set<String> fields, String prefix, List<?> before,
                                  List<?> after, String key) {
        Map<Object, Map<?, ?>> oldItems = keyedItems(before, key);
        Map<Object, Map<?, ?>> newItems = keyedItems(after, key);
        if (!new ArrayList<>(oldItems.keySet()).equals(new ArrayList<>(newItems.keySet()))) {
            fields.add(prefix);
        }
        Set<Object> ids = new java.util.HashSet<>(oldItems.keySet());
        ids.addAll(newItems.keySet());
        for (Object id : ids) {
            String itemPath = prefix + "/" + escape(key + "=" + id);
            Map<?, ?> oldItem = oldItems.get(id), newItem = newItems.get(id);
            if (oldItem == null || newItem == null) {
                fields.add(itemPath);
                continue;
            }
            Set<String> names = new TreeSet<>();
            oldItem.keySet().stream().filter(String.class::isInstance)
                    .map(String.class::cast).forEach(names::add);
            newItem.keySet().stream().filter(String.class::isInstance)
                    .map(String.class::cast).forEach(names::add);
            for (String name : names) {
                if (oldItem.containsKey(name) != newItem.containsKey(name)
                        || !Objects.equals(oldItem.get(name), newItem.get(name))) {
                    fields.add(itemPath + "/" + escape(name));
                }
            }
        }
    }

    private Map<Object, Map<?, ?>> keyedItems(List<?> items, String key) {
        Map<Object, Map<?, ?>> result = new LinkedHashMap<>();
        for (Object item : items) {
            Map<?, ?> map = (Map<?, ?>) item;
            result.put(map.get(key), map);
        }
        return result;
    }

    private boolean overlappingPath(String left, String right) {
        return left.equals(right) || left.startsWith(right + "/") || right.startsWith(left + "/");
    }

    private String escape(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private ImpactV2EvidenceReport.CategoryResult unresolvedDeliveredEvents(
            ScenarioExecutionReport execution,
            List<ImpactEvidence.EventDelivery> deliveries,
            List<ImpactEvidence.CoverageGap> gaps) {
        List<ImpactV2EvidenceReport.UnknownReason> unknowns = gapUnknowns(UNRESOLVED_DELIVERED_EVENT, gaps,
                Set.of("BEFORE_EVENT", "AFTER_EVENT", "EVENT_DELIVERY", "HORIZON_EVENT", "ATTEMPT",
                        "OBSERVER_CALLBACK", "OBSERVER_ENABLEMENT"));
        Set<ImpactV2EvidenceReport.Candidate> candidates = new TreeSet<>(CANDIDATE_ORDER);
        List<ImpactV2EvidenceReport.Finding> findings = new ArrayList<>();
        List<ScenarioExecutionReport.ActionOutcome> successfulActions = execution.actualActions().stream()
                .filter(action -> "EVENT_CONSEQUENCE".equals(action.kind()) && "COMPLETED".equals(action.status()))
                .sorted(Comparator.comparingInt(ScenarioExecutionReport.ActionOutcome::actualPosition)).toList();
        for (ScenarioExecutionReport.ActionOutcome action : successfulActions) {
            Integer eventId = action.eventEvidence() == null ? null : action.eventEvidence().eventId();
            List<ImpactEvidence.EventDelivery> matching = deliveries.stream()
                    .filter(delivery -> exactDeliveryMatch(execution, action, delivery)).toList();
            if (matching.size() != 1) {
                candidates.add(new ImpactV2EvidenceReport.Candidate(null, eventId));
                unknowns.add(unknown(UNRESOLVED_DELIVERED_EVENT,
                        matching.isEmpty() ? "DELIVERY_EVIDENCE_UNAVAILABLE" : "AMBIGUOUS_DELIVERY_EVIDENCE",
                        String.valueOf(eventId), null, eventId));
                continue;
            }
            ImpactEvidence.EventDelivery delivery = matching.get(0);
            ImpactEvidence.AggregateIdentity identity = delivery.receiverBefore() == null
                    ? null : delivery.receiverBefore().identity();
            candidates.add(new ImpactV2EvidenceReport.Candidate(identity, delivery.eventId()));
            if (identity == null || delivery.receiverAfter() == null
                    || !identity.equals(delivery.receiverAfter().identity())) {
                unknowns.add(unknown(UNRESOLVED_DELIVERED_EVENT, "RECEIVER_IDENTITY_UNAVAILABLE",
                        String.valueOf(delivery.eventId()), identity, delivery.eventId()));
                continue;
            }
            if (!delivery.eligibleBefore()) {
                unknowns.add(unknown(UNRESOLVED_DELIVERED_EVENT, "PRE_DELIVERY_ELIGIBILITY_UNCONFIRMED",
                        String.valueOf(delivery.eventId()), identity, delivery.eventId()));
                continue;
            }
            if (eventEvidenceIncomplete(gaps)) continue;
            if (!samePersistentState(delivery.receiverBefore(), delivery.receiverAfter())) continue;
            ImpactEvidence.AggregateSnapshot receiverFinal = delivery.receiverFinal();
            if (receiverFinal == null || delivery.eligibleAtHorizon() == null) {
                unknowns.add(unknown(UNRESOLVED_DELIVERED_EVENT, "FINAL_RECEIVER_OBSERVATION_UNAVAILABLE",
                        String.valueOf(delivery.eventId()), identity, delivery.eventId()));
                continue;
            }
            if (!identity.equals(receiverFinal.identity())) {
                unknowns.add(unknown(UNRESOLVED_DELIVERED_EVENT, "FINAL_RECEIVER_IDENTITY_MISMATCH",
                        String.valueOf(delivery.eventId()), identity, delivery.eventId()));
                continue;
            }
            if ("DELETED".equals(receiverFinal.lifecycleState())
                    || !Boolean.TRUE.equals(delivery.eligibleAtHorizon())) continue;
            findings.add(new ImpactV2EvidenceReport.Finding(UNRESOLVED_DELIVERED_EVENT,
                    "DELIVERED_EVENT_LEFT_RECEIVER_UNCHANGED_AND_ELIGIBLE_AT_HORIZON", identity, null,
                    delivery.eventId(), List.of(action.actionId()),
                    versions(delivery.receiverBefore(), delivery.receiverAfter(), receiverFinal)));
        }
        return category(UNRESOLVED_DELIVERED_EVENT, candidates, findings, unknowns);
    }

    private boolean exactDeliveryMatch(ScenarioExecutionReport execution,
                                       ScenarioExecutionReport.ActionOutcome action,
                                       ImpactEvidence.EventDelivery delivery) {
        ScenarioExecutionReport.EventRuntimeEvidence expected = action.eventEvidence();
        ImpactEvidence.Writer writer = delivery.writer();
        ImpactEvidence.AggregateIdentity receiver = delivery.receiverBefore() == null
                ? null : delivery.receiverBefore().identity();
        return expected != null && writer != null
                && "EVENT_CONSUMER".equals(writer.kind()) && "EVENT".equals(writer.phase())
                && Objects.equals(execution.executionAttemptId(), writer.executionAttemptId())
                && Objects.equals(execution.workloadPlanId(), writer.workloadPlanId())
                && Objects.equals(action.actionId(), writer.actionId())
                && Objects.equals(expected.eventId(), delivery.eventId())
                && Objects.equals(expected.eventTypeFqn(), delivery.eventType())
                && Objects.equals(expected.publisherAggregateId(), delivery.publisherAggregateId())
                && Objects.equals(expected.publisherAggregateVersion(), delivery.publisherAggregateVersion())
                && receiver != null
                && Objects.equals(expected.subscriberAggregateId(), receiver.aggregateId());
    }

    private boolean ownedFailedSagaWrite(ImpactEvidence.Writer writer, String attemptId,
                                         String workloadPlanId, String sagaInstanceId) {
        return writer != null && "SAGA".equals(writer.kind())
                && attemptId.equals(writer.executionAttemptId())
                && Objects.equals(workloadPlanId, writer.workloadPlanId())
                && sagaInstanceId.equals(writer.sagaInstanceId())
                && ("FORWARD".equals(writer.phase()) || "RECOVERY".equals(writer.phase()));
    }

    private boolean samePersistentState(ImpactEvidence.AggregateSnapshot left,
                                        ImpactEvidence.AggregateSnapshot right) {
        if (left == null || right == null) return left == right;
        return Objects.equals(left.identity(), right.identity())
                && Objects.equals(left.lifecycleState(), right.lifecycleState())
                && Objects.equals(left.applicationData(), right.applicationData());
    }

    private ImpactV2EvidenceReport.CategoryResult category(
            String name,
            Collection<ImpactV2EvidenceReport.Candidate> candidates,
            List<ImpactV2EvidenceReport.Finding> findings,
            List<ImpactV2EvidenceReport.UnknownReason> unknowns) {
        List<ImpactV2EvidenceReport.Finding> orderedFindings = findings.stream()
                .sorted(Comparator.comparing(ImpactV2EvidenceReport.Finding::affectedObject, IDENTITY_ORDER)
                        .thenComparing(ImpactV2EvidenceReport.Finding::eventId,
                                Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(ImpactV2EvidenceReport.Finding::reason))
                .toList();
        List<ImpactV2EvidenceReport.UnknownReason> orderedUnknowns = unknowns.stream().distinct()
                .sorted(Comparator.comparing(ImpactV2EvidenceReport.UnknownReason::subject,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ImpactV2EvidenceReport.UnknownReason::reason))
                .toList();
        long positiveObjects = orderedFindings.stream().map(ImpactV2EvidenceReport.Finding::affectedObject)
                .distinct().count();
        return new ImpactV2EvidenceReport.CategoryResult(name,
                orderedUnknowns.isEmpty() ? "COMPLETE" : "PARTIAL", candidates.size(),
                candidates.stream().sorted(CANDIDATE_ORDER).toList(), Math.toIntExact(positiveObjects),
                orderedFindings, orderedUnknowns);
    }

    private List<ImpactV2EvidenceReport.UnknownReason> gapUnknowns(
            String category, List<ImpactEvidence.CoverageGap> gaps, Set<String> stages) {
        return gaps.stream()
                .filter(gap -> stages.contains(gap.stage()) || unknownStage(gap.stage()))
                .filter(gap -> !"FRAMEWORK_METADATA_UNAVAILABLE".equals(gap.reason()))
                .filter(gap -> !DELETED_DEPENDENCY.equals(category) || invalidatesDeletedDependency(gap))
                .map(gap -> unknown(category, gap.reason(), gap.subject(), null, eventId(gap)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private boolean invalidatesDeletedDependency(ImpactEvidence.CoverageGap gap) {
        if (!"SNAPSHOT".equals(gap.stage())) return true;
        return Set.of("MISSING_AGGREGATE_IDENTITY", "MISSING_DEPENDENCY_DECLARATIONS",
                "DEPENDENCY_ENUMERATION_FAILED", "DEPENDENCY_TARGET_NOT_FOUND",
                "AMBIGUOUS_DEPENDENCY_TARGET").contains(gap.reason());
    }

    private boolean invalidatesPersistentComparison(ImpactEvidence.CoverageGap gap) {
        return !"FRAMEWORK_METADATA_UNAVAILABLE".equals(gap.reason())
                && Set.of("SNAPSHOT", "WRITE", "ATTEMPT", "OBSERVER_CALLBACK", "OBSERVER_ENABLEMENT")
                .contains(gap.stage());
    }

    private boolean eventEvidenceIncomplete(List<ImpactEvidence.CoverageGap> gaps) {
        return gaps.stream().filter(gap -> Set.of("BEFORE_EVENT", "AFTER_EVENT", "EVENT_DELIVERY", "HORIZON_EVENT")
                        .contains(gap.stage()))
                .findAny().isPresent();
    }

    private boolean unknownStage(String stage) {
        return !Set.of("SNAPSHOT", "WRITE", "BEFORE_EVENT", "AFTER_EVENT", "EVENT_DELIVERY",
                "HORIZON_EVENT", "ATTEMPT", "OBSERVER_CALLBACK", "OBSERVER_ENABLEMENT").contains(stage);
    }

    private Integer eventId(ImpactEvidence.CoverageGap gap) {
        if (!"EVENT_DELIVERY".equals(gap.stage())) return null;
        try {
            return Integer.valueOf(gap.subject());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ImpactV2EvidenceReport.UnknownReason unknown(
            String category, String reason, String subject,
            ImpactEvidence.AggregateIdentity affectedObject, Integer eventId) {
        return new ImpactV2EvidenceReport.UnknownReason(category, reason, subject, affectedObject, eventId);
    }

    private List<String> actionIds(List<ImpactEvidence.CommittedWrite> writes) {
        return writes.stream().map(ImpactEvidence.CommittedWrite::writer).filter(Objects::nonNull)
                .map(ImpactEvidence.Writer::actionId).filter(Objects::nonNull).distinct().sorted().toList();
    }

    private List<Long> versions(ImpactEvidence.AggregateSnapshot... snapshots) {
        List<Long> result = new ArrayList<>();
        for (ImpactEvidence.AggregateSnapshot snapshot : snapshots) {
            if (snapshot != null && snapshot.version() != null) result.add(snapshot.version());
        }
        return result.stream().distinct().sorted().toList();
    }

    private List<Long> versions(ImpactEvidence.AggregateSnapshot baseline,
                                ImpactEvidence.AggregateSnapshot finalState,
                                List<ImpactEvidence.CommittedWrite> writes) {
        List<Long> result = new ArrayList<>();
        if (baseline != null && baseline.version() != null) result.add(baseline.version());
        writes.stream().map(ImpactEvidence.CommittedWrite::aggregate).filter(Objects::nonNull)
                .map(ImpactEvidence.AggregateSnapshot::version).filter(Objects::nonNull).forEach(result::add);
        if (finalState != null && finalState.version() != null) result.add(finalState.version());
        return result.stream().distinct().sorted().toList();
    }

    private List<ImpactV2EvidenceReport.CategoryResult> terminalCategories(String status) {
        return List.of(DELETED_DEPENDENCY, FAILED_OPERATION_RESIDUAL, UNRESOLVED_DELIVERED_EVENT).stream()
                .map(category -> new ImpactV2EvidenceReport.CategoryResult(
                        category, status, 0, List.of(), 0, List.of(), List.of()))
                .toList();
    }

    static ImpactV2EvidenceReport assessmentFailure(
            ScenarioExecutionReport execution,
            String attemptId,
            String workloadPlanId,
            String faultScenarioId,
            String collectionStatus,
            String collectionReason,
            List<ImpactEvidence.AggregateSnapshot> baseline,
            List<ImpactEvidence.AggregateSnapshot> finalState,
            List<ImpactEvidence.CommittedWrite> writes,
            List<ImpactEvidence.EventDelivery> deliveries,
            List<ImpactEvidence.CoverageGap> gaps) {
        List<ImpactV2EvidenceReport.CategoryResult> categories = List.of(
                DELETED_DEPENDENCY, FAILED_OPERATION_RESIDUAL, UNRESOLVED_DELIVERED_EVENT).stream()
                .map(category -> new ImpactV2EvidenceReport.CategoryResult(
                        category, "UNAVAILABLE", 0, List.of(), 0, List.of(), List.of()))
                .toList();
        return new ImpactV2EvidenceReport(null, attemptId, execution.packageManifestPath(), workloadPlanId,
                faultScenarioId, execution.terminalStatus(), execution.scheduleConformance(), "UNAVAILABLE",
                "ASSESSMENT_FAILED", collectionStatus, collectionReason,
                "FINAL_SCHEDULED_ACTION", null, null, categories,
                baseline, finalState, writes, deliveries, gaps, ImpactV2EvidenceReport.RESIDUAL_POLICY);
    }

    private ImpactV2EvidenceReport report(
            ScenarioExecutionReport execution,
            String attemptId,
            String workloadPlanId,
            String faultScenarioId,
            String assessmentStatus,
            String assessmentReason,
            String collectionStatus,
            String collectionReason,
            String horizon,
            Integer completeScore,
            Integer observedAffectedObjectCount,
            List<ImpactV2EvidenceReport.CategoryResult> categories,
            List<ImpactEvidence.AggregateSnapshot> baseline,
            List<ImpactEvidence.AggregateSnapshot> finalState,
            List<ImpactEvidence.CommittedWrite> writes,
            List<ImpactEvidence.EventDelivery> deliveries,
            List<ImpactEvidence.CoverageGap> gaps) {
        return new ImpactV2EvidenceReport(null, attemptId, execution.packageManifestPath(), workloadPlanId,
                faultScenarioId, execution.terminalStatus(), execution.scheduleConformance(), assessmentStatus,
                assessmentReason, collectionStatus, collectionReason, horizon, completeScore,
                observedAffectedObjectCount, categories, baseline, finalState, writes, deliveries, gaps,
                ImpactV2EvidenceReport.RESIDUAL_POLICY);
    }

    private boolean valid(ScenarioExecutionReport report) {
        return List.of("SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED").contains(report.terminalStatus())
                && List.of("EXACT", "DEVIATED").contains(report.scheduleConformance());
    }

    private static final class EvidenceIndex {
        private final List<ImpactEvidence.AggregateSnapshot> finalSnapshots;
        private final Map<ImpactEvidence.AggregateIdentity, ImpactEvidence.AggregateSnapshot> baselineById;
        private final Map<ImpactEvidence.AggregateIdentity, ImpactEvidence.AggregateSnapshot> finalById;
        private final List<ImpactEvidence.CommittedWrite> writes;
        private final Map<ImpactEvidence.AggregateIdentity, List<ImpactEvidence.CommittedWrite>> writesById;

        private EvidenceIndex(List<ImpactEvidence.AggregateSnapshot> baseline,
                              List<ImpactEvidence.AggregateSnapshot> finalState,
                              List<ImpactEvidence.CommittedWrite> writes) {
            baselineById = snapshotsById(baseline);
            finalById = snapshotsById(finalState);
            finalSnapshots = finalById.values().stream().sorted(
                    Comparator.comparing(ImpactEvidence.AggregateSnapshot::identity, IDENTITY_ORDER)).toList();
            this.writes = writes.stream().filter(write -> write.aggregate() != null
                            && write.aggregate().identity() != null)
                    .sorted(Comparator.comparingLong(ImpactEvidence.CommittedWrite::sequence)).toList();
            Map<ImpactEvidence.AggregateIdentity, List<ImpactEvidence.CommittedWrite>> grouped = new TreeMap<>(IDENTITY_ORDER);
            this.writes.forEach(write -> grouped.computeIfAbsent(write.aggregate().identity(), ignored -> new ArrayList<>())
                    .add(write));
            Map<ImpactEvidence.AggregateIdentity, List<ImpactEvidence.CommittedWrite>> immutable = new TreeMap<>(IDENTITY_ORDER);
            grouped.forEach((identity, values) -> immutable.put(identity, List.copyOf(values)));
            writesById = java.util.Collections.unmodifiableMap(immutable);
        }

        private static Map<ImpactEvidence.AggregateIdentity, ImpactEvidence.AggregateSnapshot> snapshotsById(
                List<ImpactEvidence.AggregateSnapshot> snapshots) {
            Map<ImpactEvidence.AggregateIdentity, ImpactEvidence.AggregateSnapshot> result = new TreeMap<>(IDENTITY_ORDER);
            snapshots.stream().filter(snapshot -> snapshot != null && snapshot.identity() != null)
                    .sorted(Comparator.comparing(ImpactEvidence.AggregateSnapshot::version,
                            Comparator.nullsFirst(Long::compareTo)))
                    .forEach(snapshot -> result.put(snapshot.identity(), snapshot));
            return java.util.Collections.unmodifiableMap(result);
        }

        List<ImpactEvidence.AggregateSnapshot> finalSnapshots() { return finalSnapshots; }
        Map<ImpactEvidence.AggregateIdentity, ImpactEvidence.AggregateSnapshot> baselineById() { return baselineById; }
        Map<ImpactEvidence.AggregateIdentity, ImpactEvidence.AggregateSnapshot> finalById() { return finalById; }
        List<ImpactEvidence.CommittedWrite> writes() { return writes; }
        Map<ImpactEvidence.AggregateIdentity, List<ImpactEvidence.CommittedWrite>> writesById() { return writesById; }
    }
}
