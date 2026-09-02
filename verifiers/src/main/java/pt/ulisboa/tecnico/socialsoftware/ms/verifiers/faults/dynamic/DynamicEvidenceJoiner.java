package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicAttributionLink;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicObservation;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputOwner;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Normalizes runtime events and attributes one test/Saga invocation at a time. */
public final class DynamicEvidenceJoiner {
    public static final List<String> OBSERVATION_KINDS = List.of(
            "stepStarted", "stepFinished", "commandSent", "aggregateAccessed", "invariantViolation");
    public static final List<String> ATTRIBUTION_STATUSES = List.of(
            "exactInput", "testAndShape", "shapeOnly", "ambiguous", "unmatched");

    private static final Map<String, String> KIND_NAMES = Map.of(
            "STEP_STARTED", "stepStarted",
            "STEP_FINISHED", "stepFinished",
            "COMMAND_SENT", "commandSent",
            "AGGREGATE_ACCESSED", "aggregateAccessed",
            "INVARIANT_VIOLATION", "invariantViolation");

    public DynamicEvidenceJoinResult join(List<WorkloadPlan> workloads, List<DynamicEvidenceEvent> events) {
        return join(workloads, events, 0, List.of(), 0L, Set.of(), Map.of());
    }

    public DynamicEvidenceJoinResult join(List<WorkloadPlan> workloads,
                                          List<DynamicEvidenceEvent> events,
                                          int evidenceFilesRead,
                                          List<String> readerDiagnostics) {
        return join(workloads, events, evidenceFilesRead, readerDiagnostics, 0L, Set.of(), Map.of());
    }

    public DynamicEvidenceJoinResult join(List<WorkloadPlan> workloads,
                                          List<DynamicEvidenceEvent> events,
                                          int evidenceFilesRead,
                                          List<String> readerDiagnostics,
                                          long evidenceBytesRead,
                                          Set<String> selectedTestClasses,
                                          Map<String, String> testRunStatusByClass) {
        List<WorkloadPlan> plans = workloads == null ? List.of() : workloads.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(WorkloadPlan::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
        List<DynamicEvidenceEvent> raw = events == null ? List.of() : events;
        List<String> diagnostics = new ArrayList<>(readerDiagnostics == null ? List.of() : readerDiagnostics);
        Index index = Index.from(plans);

        LinkedHashMap<String, Normalized> byId = new LinkedHashMap<>();
        for (DynamicEvidenceEvent event : raw.stream().sorted(EVENT_ORDER).toList()) {
            Normalized normalized = normalize(event, index, diagnostics);
            if (normalized == null) continue;
            Normalized previous = byId.putIfAbsent(normalized.observation().id(), normalized);
            if (previous != null && !previous.observation().equals(normalized.observation())) {
                diagnostics.add("Conflicting duplicate runtime observation id " + normalized.observation().id()
                        + "; the first occurrence was retained");
            }
        }
        List<Normalized> normalized = List.copyOf(byId.values());
        List<DynamicObservation> observations = normalized.stream().map(Normalized::observation).toList();
        List<DynamicAttributionLink> attributions = attribute(normalized, index);

        if (!observations.isEmpty() && observations.stream().noneMatch(observation -> observation.input() != null)) {
            diagnostics.add("Known runtime input-map mismatch remains visible: verifier entries use workloadPlanIds "
                    + "while the simulator reader expects scenarioPlanIds; no exact input id was observed");
        }
        addContextDiagnostics(observations, diagnostics);
        Map<String, Object> accounting = accounting(plans, observations, attributions,
                selectedTestClasses, testRunStatusByClass);
        return new DynamicEvidenceJoinResult(observations, attributions, accounting, diagnostics,
                evidenceFilesRead, evidenceBytesRead);
    }

    private Normalized normalize(DynamicEvidenceEvent event, Index index, List<String> diagnostics) {
        if (event == null || blank(event.eventId()) || blank(event.eventKind()) || event.sequence() == null
                || blank(event.timestamp()) || blank(event.threadName())) {
            diagnostics.add(location(event) + ": runtime event omitted because id, kind, sequence, timestamp, or thread is missing");
            return null;
        }
        String kind = KIND_NAMES.get(event.eventKind());
        if (kind == null) {
            diagnostics.add(location(event) + ": unsupported runtime event kind " + event.eventKind());
            return null;
        }
        String saga = resolveSaga(event, index);
        String step = resolveStep(event.stepName(), saga, index);
        String exactInput = !blank(event.inputVariantId()) && index.inputsById().containsKey(event.inputVariantId())
                && (saga == null || Objects.equals(saga, index.inputsById().get(event.inputVariantId()).sagaFqn()))
                ? event.inputVariantId() : null;
        String execution = testExecution(event);
        DynamicObservation.TestIdentity test = blank(execution) ? null
                : new DynamicObservation.TestIdentity(execution, event.testClassFqn(), event.testMethodName());

        String phase = null;
        String outcome = null;
        DynamicObservation.ErrorDetail error = null;
        DynamicObservation.CommandDetail command = null;
        DynamicObservation.AccessDetail access = null;
        DynamicObservation.ViolationDetail violation = null;
        if ("stepStarted".equals(kind)) {
            phase = lower(event.payloadText("stepPhase"));
        } else if ("stepFinished".equals(kind)) {
            outcome = lower(event.payloadText("outcome"));
            if (!blank(event.payloadText("errorType")) || !blank(event.payloadText("errorMessage"))) {
                error = new DynamicObservation.ErrorDetail(event.payloadText("errorType"), event.payloadText("errorMessage"));
            }
        } else if ("commandSent".equals(kind)) {
            command = new DynamicObservation.CommandDetail(event.payloadText("commandType"), event.payloadMap("fields"));
        } else if ("aggregateAccessed".equals(kind)) {
            access = new DynamicObservation.AccessDetail(event.payloadText("aggregateType"),
                    event.payloadText("aggregateId"), lower(event.payloadText("accessMode")));
        } else if ("invariantViolation".equals(kind)) {
            violation = new DynamicObservation.ViolationDetail("aggregateInvariant",
                    event.payloadText("exceptionMessage"));
        }
        DynamicObservation observation = new DynamicObservation(event.eventId(), kind, event.sequence(),
                event.timestamp(), event.threadName(), test, saga, event.functionalityInvocationId(), step,
                exactInput, phase, outcome, error, command, access, violation);
        return new Normalized(observation, event);
    }

    private List<DynamicAttributionLink> attribute(List<Normalized> observations, Index index) {
        LinkedHashMap<GroupKey, List<Normalized>> groups = new LinkedHashMap<>();
        observations.stream()
                .filter(value -> value.observation().test() != null
                        && !blank(value.observation().saga()) && !blank(value.observation().invocation()))
                .forEach(value -> groups.computeIfAbsent(new GroupKey(
                        value.observation().test().execution(), value.observation().saga(),
                        value.observation().invocation()), ignored -> new ArrayList<>()).add(value));
        return groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> attribution(entry.getKey(), entry.getValue(), index))
                .toList();
    }

    private DynamicAttributionLink attribution(GroupKey key, List<Normalized> group, Index index) {
        List<String> observationIds = group.stream().map(value -> value.observation().id()).distinct().sorted().toList();
        Set<String> exact = group.stream().map(value -> value.observation().input()).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (exact.size() == 1) return link(key, "exactInput", observationIds, exact.iterator().next(), List.of(), null);
        if (exact.size() > 1) return link(key, "ambiguous", observationIds, null, sorted(exact), null);

        Set<String> observedSteps = group.stream().map(value -> value.observation().step())
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List<InputVariant> shape = observedSteps.isEmpty() ? List.of()
                : index.inputsBySaga().getOrDefault(key.saga(), List.of()).stream()
                .filter(input -> index.stepsByInput().getOrDefault(input.deterministicId(), Set.of())
                        .containsAll(observedSteps))
                .toList();
        DynamicEvidenceEvent representative = group.get(0).raw();
        List<InputVariant> testAndShape = shape.stream().filter(input -> testMatches(input, representative)).toList();
        if (testAndShape.size() == 1) {
            return link(key, "testAndShape", observationIds, testAndShape.get(0).deterministicId(), List.of(), null);
        }
        if (testAndShape.size() > 1) {
            return link(key, "ambiguous", observationIds, null, inputIds(testAndShape), null);
        }
        if (shape.size() == 1) {
            return link(key, "shapeOnly", observationIds, shape.get(0).deterministicId(), List.of(), null);
        }
        if (shape.size() > 1) {
            return link(key, "ambiguous", observationIds, null, inputIds(shape), null);
        }
        return link(key, "unmatched", observationIds, null, List.of(), "no-static-input");
    }

    private boolean testMatches(InputVariant input, DynamicEvidenceEvent event) {
        if (blank(event.testClassFqn())) return false;
        Set<String> runtimeNames = java.util.stream.Stream.of(event.testMethodName(), event.testDisplayName())
                .filter(value -> !blank(value)).collect(Collectors.toSet());
        if (runtimeNames.isEmpty()) return false;
        if (Objects.equals(input.sourceClassFqn(), event.testClassFqn())
                && (runtimeNames.contains(input.sourceMethodName())
                || runtimeNames.contains(input.callContextMethodName()))) return true;
        return input.owners().stream().anyMatch(owner -> ownerMatches(owner, event));
    }

    private boolean ownerMatches(InputOwner owner, DynamicEvidenceEvent event) {
        return Objects.equals(owner.testClassFqn(), event.testClassFqn())
                && (Objects.equals(owner.testMethodName(), event.testMethodName())
                || Objects.equals(owner.testMethodName(), event.testDisplayName()));
    }

    private DynamicAttributionLink link(GroupKey key, String status, List<String> observations,
                                        String input, List<String> candidates, String reason) {
        return new DynamicAttributionLink(key.testExecution(), key.saga(), key.invocation(), status,
                observations, input, candidates, reason);
    }

    private Map<String, Object> accounting(List<WorkloadPlan> workloads,
                                           List<DynamicObservation> observations,
                                           List<DynamicAttributionLink> attributions,
                                           Set<String> selectedTestClasses,
                                           Map<String, String> testRunStatusByClass) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        Map<String, String> statuses = testRunStatusByClass == null ? Map.of() : testRunStatusByClass;
        Set<String> selected = selectedTestClasses == null ? Set.of() : selectedTestClasses;
        int passed = (int) selected.stream().filter(test -> "PASSED".equals(statuses.get(test))).count();
        int failed = (int) selected.stream().filter(test -> {
            String status = statuses.get(test);
            return status != null && !"PASSED".equals(status) && !"SKIPPED".equals(status);
        }).count();
        result.put("testOutcomes", orderedCounts(Map.of("passed", passed, "failed", failed), "passed", "failed"));

        LinkedHashMap<String, Integer> kinds = zeroCounts(OBSERVATION_KINDS);
        observations.forEach(observation -> kinds.merge(observation.kind(), 1, Integer::sum));
        LinkedHashMap<String, Object> observationCounts = new LinkedHashMap<>();
        observationCounts.put("total", observations.size());
        observationCounts.put("byKind", kinds);
        observationCounts.put("withoutTestContext", observations.stream().filter(value -> value.test() == null).count());
        result.put("observations", observationCounts);

        LinkedHashMap<String, Integer> attributionCounts = zeroCounts(ATTRIBUTION_STATUSES);
        attributions.forEach(link -> attributionCounts.merge(link.status(), 1, Integer::sum));
        result.put("sagaInvocations", Map.of("total", attributions.size(), "byStatus", attributionCounts));

        Map<String, String> strongest = new LinkedHashMap<>();
        for (DynamicAttributionLink link : attributions) {
            if (link.input() == null || !List.of("exactInput", "testAndShape", "shapeOnly").contains(link.status())) continue;
            strongest.compute(link.input(), (ignored, prior) -> stronger(prior, link.status()));
        }
        LinkedHashMap<String, Integer> unique = zeroCounts(List.of("exactInput", "testAndShape", "shapeOnly"));
        strongest.values().forEach(status -> unique.merge(status, 1, Integer::sum));
        result.put("uniqueInputEvidence", unique);
        result.put("workloadParticipantEvidence", participantEvidence(workloads, attributions));
        return result;
    }

    private Map<String, Integer> participantEvidence(List<WorkloadPlan> workloads,
                                                     List<DynamicAttributionLink> attributions) {
        LinkedHashMap<String, Integer> counts = zeroCounts(List.of(
                "allInputsObservedInOneCommonTest", "allInputsObservedAcrossSeparateTests",
                "someInputsObserved", "noInputsObserved"));
        Map<String, Set<String>> testsByInput = new LinkedHashMap<>();
        attributions.stream().filter(link -> link.input() != null
                        && ("exactInput".equals(link.status()) || "testAndShape".equals(link.status())))
                .forEach(link -> testsByInput.computeIfAbsent(link.input(), ignored -> new LinkedHashSet<>())
                        .add(link.testExecution()));
        for (WorkloadPlan workload : workloads) {
            List<String> inputs = workload.participants().stream().map(participant -> participant.inputVariantId())
                    .filter(Objects::nonNull).distinct().toList();
            long observed = inputs.stream().filter(testsByInput::containsKey).count();
            if (observed == 0) counts.merge("noInputsObserved", 1, Integer::sum);
            else if (observed < inputs.size()) counts.merge("someInputsObserved", 1, Integer::sum);
            else {
                Set<String> common = new LinkedHashSet<>(testsByInput.get(inputs.get(0)));
                for (String input : inputs.subList(1, inputs.size())) common.retainAll(testsByInput.get(input));
                counts.merge(common.isEmpty() ? "allInputsObservedAcrossSeparateTests"
                        : "allInputsObservedInOneCommonTest", 1, Integer::sum);
            }
        }
        return counts;
    }

    private String resolveSaga(DynamicEvidenceEvent event, Index index) {
        List<String> identities = java.util.stream.Stream.of(event.functionalityClassFqn(), event.functionalityClassSimpleName(),
                event.functionalityName()).filter(value -> !blank(value)).toList();
        Set<String> knownSagas = new LinkedHashSet<>(index.inputsBySaga().keySet());
        knownSagas.addAll(index.stepsBySaga().keySet());
        for (String identity : identities) {
            if (knownSagas.contains(identity)) return identity;
            List<String> matches = knownSagas.stream()
                    .filter(saga -> Objects.equals(simpleName(saga), simpleName(identity))).sorted().toList();
            if (matches.size() == 1) return matches.get(0);
        }
        return null;
    }

    private String resolveStep(String runtimeStep, String saga, Index index) {
        if (blank(runtimeStep) || blank(saga)) return null;
        List<String> steps = index.stepsBySaga().getOrDefault(saga, List.of());
        if (steps.contains(runtimeStep)) return runtimeStep;
        List<String> matches = steps.stream().filter(step -> Objects.equals(stripOccurrence(step), stripOccurrence(runtimeStep))).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private String testExecution(DynamicEvidenceEvent event) {
        if (!blank(event.testUniqueId())) return event.testUniqueId();
        if (!blank(event.testClassFqn()) && !blank(event.testMethodName())) {
            return event.testClassFqn() + "#" + event.testMethodName();
        }
        return null;
    }

    private void addContextDiagnostics(List<DynamicObservation> observations, List<String> diagnostics) {
        long withoutTest = observations.stream().filter(value -> value.test() == null).count();
        long withoutSaga = observations.stream().filter(value -> value.saga() == null).count();
        long withoutInvocation = observations.stream().filter(value -> value.invocation() == null).count();
        long withoutStep = observations.stream().filter(value -> value.step() == null).count();
        if (withoutTest > 0) diagnostics.add(withoutTest + " observations lack test execution context");
        if (withoutSaga > 0) diagnostics.add(withoutSaga + " observations lack a uniquely resolved Saga");
        if (withoutInvocation > 0) diagnostics.add(withoutInvocation + " observations lack a Saga invocation");
        if (withoutStep > 0) diagnostics.add(withoutStep + " observations lack a uniquely resolved Saga-local step");
    }

    private String stronger(String prior, String candidate) {
        if (prior == null) return candidate;
        return rank(candidate) > rank(prior) ? candidate : prior;
    }

    private int rank(String status) {
        return switch (status) { case "exactInput" -> 3; case "testAndShape" -> 2; case "shapeOnly" -> 1; default -> 0; };
    }

    private LinkedHashMap<String, Integer> zeroCounts(List<String> names) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        names.forEach(name -> counts.put(name, 0));
        return counts;
    }

    private LinkedHashMap<String, Integer> orderedCounts(Map<String, Integer> source, String... keys) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (String key : keys) result.put(key, source.getOrDefault(key, 0));
        return result;
    }

    private List<String> inputIds(List<InputVariant> inputs) {
        return inputs.stream().map(InputVariant::deterministicId).filter(Objects::nonNull).distinct().sorted().toList();
    }

    private List<String> sorted(Set<String> values) { return values.stream().sorted().toList(); }
    private String stripOccurrence(String value) { return value == null ? null : value.replaceFirst("#\\d+$", ""); }
    private String simpleName(String value) { int index = value == null ? -1 : value.lastIndexOf('.'); return index < 0 ? value : value.substring(index + 1); }
    private String lower(String value) { return value == null ? null : value.toLowerCase(Locale.ROOT); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String location(DynamicEvidenceEvent event) { return event == null ? "runtime evidence" : event.sourcePath() + ":" + event.lineNumber(); }

    private static final Comparator<DynamicEvidenceEvent> EVENT_ORDER = Comparator
            .comparing(DynamicEvidenceEvent::sequence, Comparator.nullsLast(Long::compareTo))
            .thenComparing(DynamicEvidenceEvent::eventId, Comparator.nullsLast(String::compareTo))
            .thenComparing(event -> event.sourcePath() == null ? "" : event.sourcePath().toString())
            .thenComparingInt(DynamicEvidenceEvent::lineNumber);

    private record Normalized(DynamicObservation observation, DynamicEvidenceEvent raw) { }
    private record GroupKey(String testExecution, String saga, String invocation) implements Comparable<GroupKey> {
        @Override public int compareTo(GroupKey other) {
            int result = testExecution.compareTo(other.testExecution);
            if (result != 0) return result;
            result = saga.compareTo(other.saga);
            return result != 0 ? result : invocation.compareTo(other.invocation);
        }
    }

    private record Index(Map<String, InputVariant> inputsById,
                         Map<String, List<InputVariant>> inputsBySaga,
                         Map<String, List<String>> stepsBySaga,
                         Map<String, Set<String>> stepsByInput) {
        private static Index from(List<WorkloadPlan> workloads) {
            LinkedHashMap<String, InputVariant> inputs = new LinkedHashMap<>();
            LinkedHashMap<String, LinkedHashSet<String>> steps = new LinkedHashMap<>();
            LinkedHashMap<String, LinkedHashSet<String>> inputSteps = new LinkedHashMap<>();
            for (WorkloadPlan workload : workloads) {
                workload.acceptedInputs().forEach(input -> {
                    if (input != null && input.deterministicId() != null) inputs.putIfAbsent(input.deterministicId(), input);
                });
                Map<String, String> sagaByParticipant = workload.participants().stream().collect(Collectors.toMap(
                        participant -> participant.deterministicId(), participant -> participant.sagaFqn(),
                        (left, right) -> left, LinkedHashMap::new));
                Map<String, String> inputByParticipant = workload.participants().stream().collect(Collectors.toMap(
                        participant -> participant.deterministicId(), participant -> participant.inputVariantId(),
                        (left, right) -> left, LinkedHashMap::new));
                for (ScheduledStep step : workload.forwardSchedule()) {
                    String saga = sagaByParticipant.get(step.sagaInstanceId());
                    if (saga == null || step.stepId() == null) continue;
                    String local = step.stepId().contains("::") ? step.stepId().substring(step.stepId().lastIndexOf("::") + 2) : step.stepId();
                    steps.computeIfAbsent(saga, ignored -> new LinkedHashSet<>()).add(local);
                    String input = inputByParticipant.get(step.sagaInstanceId());
                    if (input != null) inputSteps.computeIfAbsent(input, ignored -> new LinkedHashSet<>()).add(local);
                }
            }
            Map<String, List<InputVariant>> bySaga = inputs.values().stream().collect(Collectors.groupingBy(
                    InputVariant::sagaFqn, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), list ->
                            list.stream().sorted(Comparator.comparing(InputVariant::deterministicId)).toList())));
            LinkedHashMap<String, List<String>> stepLists = new LinkedHashMap<>();
            steps.forEach((saga, values) -> stepLists.put(saga, values.stream().sorted().toList()));
            LinkedHashMap<String, Set<String>> inputStepSets = new LinkedHashMap<>();
            inputSteps.forEach((input, values) -> inputStepSets.put(input, Set.copyOf(values)));
            return new Index(Map.copyOf(inputs), Map.copyOf(bySaga), Map.copyOf(stepLists), Map.copyOf(inputStepSets));
        }
    }
}
