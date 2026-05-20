package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceSummary;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.EnrichedScenarioRecord;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.MatchedTestExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.ObservedAggregateAccess;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.ObservedCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.ObservedStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicEvidenceJoiner {

    public DynamicEvidenceJoinResult join(List<ScenarioPlan> scenarioPlans, List<DynamicEvidenceEvent> events) {
        return join(scenarioPlans, events, 0, List.of(), 0L);
    }

    public DynamicEvidenceJoinResult join(List<ScenarioPlan> scenarioPlans,
                                          List<DynamicEvidenceEvent> events,
                                          int evidenceFilesRead,
                                          List<String> readerWarnings) {
        return join(scenarioPlans, events, evidenceFilesRead, readerWarnings, 0L);
    }

    public DynamicEvidenceJoinResult join(List<ScenarioPlan> scenarioPlans,
                                          List<DynamicEvidenceEvent> events,
                                          int evidenceFilesRead,
                                          List<String> readerWarnings,
                                          long evidenceBytesRead) {
        List<ScenarioPlan> plans = scenarioPlans == null ? List.of() : scenarioPlans;
        List<DynamicEvidenceEvent> safeEvents = events == null ? List.of() : events;
        List<String> warnings = new ArrayList<>(readerWarnings == null ? List.of() : readerWarnings);
        CatalogIndex catalogIndex = CatalogIndex.from(plans);
        JoinIndex joinIndex = JoinIndex.build(safeEvents, event -> analyzeEvent(event, plans, catalogIndex));
        int missingContext = (int) safeEvents.stream().filter(event -> isBlank(event.testClassFqn())).count();

        List<EnrichedScenarioRecord> records = plans.stream()
                .map(plan -> enrich(plan, joinIndex))
                .toList();
        return new DynamicEvidenceJoinResult(records, warnings, safeEvents.size(), missingContext, evidenceFilesRead, evidenceBytesRead);
    }

    private EnrichedScenarioRecord enrich(ScenarioPlan plan, JoinIndex joinIndex) {
        if (joinIndex.isEmpty()) {
            return record(plan, DynamicEvidenceJoinStatus.NOT_COVERED, List.of(), List.of(), List.of());
        }

        List<String> planInputIds = plan.inputs().stream()
                .map(InputVariant::deterministicId)
                .filter(Objects::nonNull)
                .toList();

        List<DynamicEvidenceEvent> exactEvents = planInputIds.stream()
                .flatMap(inputId -> joinIndex.exactEvents(inputId).stream())
                .sorted(EVENT_ORDER)
                .toList();
        if (!exactEvents.isEmpty()) {
            List<String> matchedIds = exactEvents.stream()
                    .map(DynamicEvidenceEvent::inputVariantId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
            return record(plan, DynamicEvidenceJoinStatus.MATCHED_EXACT, matchedIds, exactEvents, List.of());
        }

        List<EventAnalysis> relevantAnalyses = joinIndex.relevantAnalyses(plan.deterministicId()).stream()
                .sorted(Comparator.comparing(EventAnalysis::event, EVENT_ORDER))
                .toList();
        if (relevantAnalyses.isEmpty()) {
            return record(plan, DynamicEvidenceJoinStatus.UNMATCHED, List.of(), List.of(), List.of());
        }

        Set<String> candidateInputIds = relevantAnalyses.stream()
                .flatMap(analysis -> analysis.candidateInputs().stream())
                .filter(ref -> Objects.equals(ref.planId(), plan.deterministicId()))
                .map(ref -> ref.input().deterministicId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> identityMatchIds = relevantAnalyses.stream()
                .flatMap(analysis -> analysis.identityMatches().stream())
                .filter(ref -> Objects.equals(ref.planId(), plan.deterministicId()))
                .map(ref -> ref.input().deterministicId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasCompleteTestIdentity = relevantAnalyses.stream().anyMatch(EventAnalysis::completeTestIdentity);
        boolean hasAmbiguousSagaIdentity = relevantAnalyses.stream().anyMatch(analysis -> analysis.candidateSagaFqns().size() > 1);

        if (hasCompleteTestIdentity) {
            if (identityMatchIds.size() == 1) {
                String matchedId = identityMatchIds.iterator().next();
                if (planInputIds.contains(matchedId)) {
                    return record(plan, DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE, List.of(matchedId), relevantEvents(relevantAnalyses), List.of());
                }
                return record(plan, DynamicEvidenceJoinStatus.UNMATCHED, List.of(), relevantEvents(relevantAnalyses), List.of());
            }
            if (identityMatchIds.size() > 1) {
                return record(plan, DynamicEvidenceJoinStatus.AMBIGUOUS, sorted(identityMatchIds), relevantEvents(relevantAnalyses), ambiguityWarnings(relevantAnalyses, candidateInputIds, identityMatchIds));
            }
            return record(plan, DynamicEvidenceJoinStatus.UNMATCHED, List.of(), relevantEvents(relevantAnalyses), List.of());
        }

        if (candidateInputIds.size() == 1) {
            if (hasAmbiguousSagaIdentity) {
                return record(plan, DynamicEvidenceJoinStatus.AMBIGUOUS, sorted(candidateInputIds), relevantEvents(relevantAnalyses), ambiguityWarnings(relevantAnalyses, candidateInputIds, identityMatchIds));
            }
            String matchedId = candidateInputIds.iterator().next();
            if (planInputIds.contains(matchedId)) {
                return record(plan, DynamicEvidenceJoinStatus.MATCHED_PARTIAL, List.of(), relevantEvents(relevantAnalyses), List.of());
            }
            return record(plan, DynamicEvidenceJoinStatus.UNMATCHED, List.of(), relevantEvents(relevantAnalyses), List.of());
        }
        if (candidateInputIds.size() > 1) {
            return record(plan, DynamicEvidenceJoinStatus.AMBIGUOUS, sorted(candidateInputIds), relevantEvents(relevantAnalyses), ambiguityWarnings(relevantAnalyses, candidateInputIds, identityMatchIds));
        }
        return record(plan, DynamicEvidenceJoinStatus.UNMATCHED, List.of(), relevantEvents(relevantAnalyses), List.of());
    }

    private List<DynamicEvidenceEvent> relevantEvents(List<EventAnalysis> analyses) {
        return analyses.stream()
                .map(EventAnalysis::event)
                .sorted(EVENT_ORDER)
                .toList();
    }

    private List<String> ambiguityWarnings(List<EventAnalysis> analyses, Set<String> candidateInputIds, Set<String> identityMatchIds) {
        List<String> warnings = new ArrayList<>();
        for (EventAnalysis analysis : analyses) {
            warnings.add(ambiguousWarning(analysis, candidateInputIds, identityMatchIds));
        }
        return warnings.stream().distinct().toList();
    }

    private String ambiguousWarning(EventAnalysis analysis, Set<String> candidateInputIds, Set<String> identityMatchIds) {
        return "Ambiguous dynamic evidence at "
                + evidenceLocation(analysis.event())
                + " for functionalityName='"
                + nullToEmpty(analysis.event().functionalityName())
                + "' step='"
                + nullToEmpty(analysis.event().stepName())
                + "'; candidateSagaFqns="
                + sorted(analysis.candidateSagaFqns())
                + "; candidateInputVariantIds="
                + sorted(candidateInputIds)
                + "; identityMatchIds="
                + sorted(identityMatchIds);
    }

    private String evidenceLocation(DynamicEvidenceEvent event) {
        String path = event.sourcePath() == null ? "<unknown>" : event.sourcePath().toString();
        return path + ":" + event.lineNumber();
    }

    private EnrichedScenarioRecord record(ScenarioPlan plan,
                                          DynamicEvidenceJoinStatus status,
                                          List<String> matchedInputIds,
                                          List<DynamicEvidenceEvent> matchedEvents,
                                          List<String> warnings) {
        DynamicEvidenceSummary summary = new DynamicEvidenceSummary(
                status,
                matchedInputIds,
                matchedTestExecutions(matchedEvents),
                observedSteps(plan, matchedEvents),
                observedAggregateAccesses(plan, matchedEvents),
                observedCommands(plan, matchedEvents),
                warnings);
        return new EnrichedScenarioRecord(EnrichedScenarioRecord.SCHEMA_VERSION, plan.deterministicId(), plan, summary);
    }

    private List<MatchedTestExecution> matchedTestExecutions(List<DynamicEvidenceEvent> events) {
        return events.stream()
                .filter(event -> !isBlank(event.testClassFqn()) || event.sourcePath() != null)
                .collect(Collectors.toMap(
                        event -> List.of(nullToEmpty(event.testClassFqn()), nullToEmpty(event.testMethodName()), nullToEmpty(event.testDisplayName()), nullToEmpty(event.testUniqueId()), event.sourcePath() == null ? "" : event.sourcePath().toString()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream()
                .map(event -> new MatchedTestExecution(event.testClassFqn(), event.testMethodName(), event.testDisplayName(), event.testUniqueId(), event.sourcePath() == null ? null : event.sourcePath().toString(), null))
                .toList();
    }

    private List<ObservedStep> observedSteps(ScenarioPlan plan, List<DynamicEvidenceEvent> events) {
        Map<String, List<DynamicEvidenceEvent>> byStep = events.stream()
                .filter(event -> !isBlank(event.stepName()) || !isBlank(event.functionalityName()))
                .collect(Collectors.groupingBy(event -> nullToEmpty(resolveSagaFqn(plan, event)) + "\u0000" + nullToEmpty(event.functionalityName()) + "\u0000" + nullToEmpty(event.stepName()), LinkedHashMap::new, Collectors.toList()));
        return byStep.values().stream()
                .map(group -> {
                    DynamicEvidenceEvent first = group.getFirst();
                    List<String> kinds = group.stream().map(DynamicEvidenceEvent::eventKind).filter(Objects::nonNull).distinct().toList();
                    List<String> outcomes = group.stream().map(event -> event.payloadText("outcome")).filter(Objects::nonNull).distinct().toList();
                    return new ObservedStep(resolveSagaFqn(plan, first), first.functionalityName(), first.stepName(), kinds, outcomes);
                })
                .toList();
    }

    private List<ObservedAggregateAccess> observedAggregateAccesses(ScenarioPlan plan, List<DynamicEvidenceEvent> events) {
        return events.stream()
                .filter(event -> "AGGREGATE_ACCESSED".equals(event.eventKind()))
                .map(event -> new ObservedAggregateAccess(resolveSagaFqn(plan, event), event.stepName(), event.payloadText("accessMode"), event.payloadText("aggregateType"), event.payloadText("aggregateId"), event.payloadText("sourceMethod"), event.eventId() == null ? List.of() : List.of(event.eventId())))
                .toList();
    }

    private List<ObservedCommand> observedCommands(ScenarioPlan plan, List<DynamicEvidenceEvent> events) {
        return events.stream()
                .filter(event -> "COMMAND_SENT".equals(event.eventKind()))
                .map(event -> new ObservedCommand(resolveSagaFqn(plan, event), event.stepName(), event.payloadText("commandType"), event.payloadText("commandFqn"), event.payloadText("serviceName"), event.payloadText("rootAggregateId"), event.eventId() == null ? List.of() : List.of(event.eventId())))
                .toList();
    }

    private EventAnalysis analyzeEvent(DynamicEvidenceEvent event, List<ScenarioPlan> plans, CatalogIndex catalogIndex) {
        Set<String> candidateSagaFqns = catalogIndex.matchingSagaFqns(event.functionalityName());
        if (candidateSagaFqns.isEmpty() || isBlank(event.stepName())) {
            return new EventAnalysis(event, candidateSagaFqns, List.of(), List.of(), hasCompleteTestIdentity(event));
        }

        List<CandidateInputRef> candidateInputs = plans.stream()
                .filter(plan -> matchesPlanStep(plan, event))
                .filter(plan -> planSagaFqns(plan).stream().anyMatch(fqn -> sagaMatches(fqn, candidateSagaFqns)))
                .flatMap(plan -> plan.inputs().stream()
                        .filter(input -> sagaMatches(input.sagaFqn(), candidateSagaFqns))
                        .map(input -> new CandidateInputRef(plan.deterministicId(), input)))
                .toList();
        List<CandidateInputRef> identityMatches = candidateInputs.stream()
                .filter(ref -> inputIdentityMatches(ref.input(), event))
                .toList();
        return new EventAnalysis(event, candidateSagaFqns, candidateInputs, identityMatches, hasCompleteTestIdentity(event));
    }

    private boolean hasCompleteTestIdentity(DynamicEvidenceEvent event) {
        return !isBlank(event.testClassFqn()) && (!isBlank(event.testMethodName()) || !isBlank(event.testDisplayName()));
    }

    private boolean inputIdentityMatches(InputVariant input, DynamicEvidenceEvent event) {
        if (isBlank(event.testClassFqn())) {
            return false;
        }
        if (!input.owners().isEmpty()) {
            return input.owners().stream().anyMatch(owner -> Objects.equals(owner.testClassFqn(), event.testClassFqn())
                    && (Objects.equals(owner.testMethodName(), event.testMethodName())
                    || Objects.equals(owner.testMethodName(), event.testDisplayName())));
        }
        if (!Objects.equals(input.sourceClassFqn(), event.testClassFqn())) {
            return false;
        }
        return !isBlank(input.sourceMethodName())
                && (Objects.equals(input.sourceMethodName(), event.testMethodName()) || Objects.equals(input.sourceMethodName(), event.testDisplayName()));
    }

    private boolean matchesPlanStep(ScenarioPlan plan, DynamicEvidenceEvent event) {
        if (isBlank(event.stepName())) {
            return false;
        }
        String runtimeStepName = event.stepName().trim();
        return plan.expandedSchedule().stream()
                .map(ScheduledStep::stepId)
                .map(this::normalizedStaticStepName)
                .anyMatch(runtimeStepName::equals);
    }

    private Set<String> planSagaFqns(ScenarioPlan plan) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        plan.sagaInstances().stream().map(SagaInstance::sagaFqn).filter(Objects::nonNull).forEach(names::add);
        plan.inputs().stream().map(InputVariant::sagaFqn).filter(Objects::nonNull).forEach(names::add);
        return names;
    }

    private boolean sagaMatches(String inputSagaFqn, Set<String> observedNames) {
        return observedNames.stream().anyMatch(name -> sagaNameMatches(inputSagaFqn, name));
    }

    private boolean sagaNameMatches(String sagaFqn, String observed) {
        return observedSagaNames(sagaFqn).contains(observed) || observedSagaNames(observed).contains(sagaFqn);
    }

    private String resolveSagaFqn(ScenarioPlan plan, DynamicEvidenceEvent event) {
        return planSagaFqns(plan).stream().filter(fqn -> sagaNameMatches(fqn, event.functionalityName())).findFirst().orElse(null);
    }

    private static String simpleName(String fqn) {
        if (fqn == null) {
            return null;
        }
        int index = fqn.lastIndexOf('.');
        return index >= 0 ? fqn.substring(index + 1) : fqn;
    }

    private String stepName(String stepId) {
        if (stepId == null) {
            return "";
        }
        int index = stepId.lastIndexOf("::");
        return index >= 0 ? stepId.substring(index + 2) : stepId;
    }

    private String normalizedStaticStepName(String stepId) {
        return stepName(stepId).trim().replaceFirst("#\\d+$", "");
    }

    private static Set<String> observedSagaNames(String sagaFqnOrName) {
        if (sagaFqnOrName == null || sagaFqnOrName.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(sagaFqnOrName);
        String simple = simpleName(sagaFqnOrName);
        names.add(simple);
        String withoutSuffix = stripSagaClassSuffix(simple);
        names.add(withoutSuffix);
        names.add(decapitalize(withoutSuffix));
        return names;
    }

    private static String stripSagaClassSuffix(String name) {
        if (name == null) {
            return null;
        }
        return name.replaceFirst("(Functionality)?Sagas$", "");
    }

    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private List<String> ids(List<InputVariant> inputs) {
        return inputs.stream().map(InputVariant::deterministicId).filter(Objects::nonNull).sorted().toList();
    }

    private List<String> sorted(Set<String> values) {
        return values.stream().filter(Objects::nonNull).sorted().toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CatalogIndex(Map<String, Set<String>> sagaFqnsByObservedName) {
        static CatalogIndex from(List<ScenarioPlan> plans) {
            Map<String, LinkedHashSet<String>> byObservedName = new LinkedHashMap<>();
            for (ScenarioPlan plan : plans) {
                for (String sagaFqn : allSagaFqns(plan)) {
                    for (String observedName : observedSagaNames(sagaFqn)) {
                        addObservedName(byObservedName, observedName, sagaFqn);
                    }
                }
            }
            Map<String, Set<String>> frozen = new LinkedHashMap<>();
            byObservedName.forEach((observedName, matches) -> frozen.put(observedName, Collections.unmodifiableSet(new LinkedHashSet<>(matches))));
            return new CatalogIndex(Collections.unmodifiableMap(frozen));
        }

        Set<String> matchingSagaFqns(String observedName) {
            if (observedName == null || observedName.isBlank()) {
                return Set.of();
            }
            return sagaFqnsByObservedName.getOrDefault(observedName, Set.of());
        }

        private static void addObservedName(Map<String, LinkedHashSet<String>> index, String observedName, String sagaFqn) {
            if (observedName == null || observedName.isBlank() || sagaFqn == null || sagaFqn.isBlank()) {
                return;
            }
            index.computeIfAbsent(observedName, key -> new LinkedHashSet<>()).add(sagaFqn);
        }

        private static Set<String> allSagaFqns(ScenarioPlan plan) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            plan.sagaInstances().stream().map(SagaInstance::sagaFqn).filter(Objects::nonNull).forEach(names::add);
            plan.inputs().stream().map(InputVariant::sagaFqn).filter(Objects::nonNull).forEach(names::add);
            return names;
        }
    }

    private record CandidateInputRef(String planId, InputVariant input) {
    }

    private record EventAnalysis(DynamicEvidenceEvent event,
                                 Set<String> candidateSagaFqns,
                                 List<CandidateInputRef> candidateInputs,
                                 List<CandidateInputRef> identityMatches,
                                 boolean completeTestIdentity) {
        EventAnalysis {
            candidateSagaFqns = candidateSagaFqns == null ? Set.of() : Set.copyOf(candidateSagaFqns);
            candidateInputs = candidateInputs == null ? List.of() : List.copyOf(candidateInputs);
            identityMatches = identityMatches == null ? List.of() : List.copyOf(identityMatches);
        }

        boolean relevantTo(String planId) {
            return candidateInputs.stream().anyMatch(ref -> Objects.equals(ref.planId(), planId));
        }
    }

    private record JoinIndex(Map<String, List<DynamicEvidenceEvent>> exactEventsByInputId,
                             Map<String, List<EventAnalysis>> relevantAnalysesByPlanId,
                             boolean hasEvents) {
        static JoinIndex build(List<DynamicEvidenceEvent> events, Function<DynamicEvidenceEvent, EventAnalysis> analyzer) {
            Map<String, List<DynamicEvidenceEvent>> exactEventsByInputId = new LinkedHashMap<>();
            Map<String, List<EventAnalysis>> relevantAnalysesByPlanId = new LinkedHashMap<>();
            for (DynamicEvidenceEvent event : events) {
                if (!isBlank(event.inputVariantId())) {
                    exactEventsByInputId.computeIfAbsent(event.inputVariantId(), ignored -> new ArrayList<>()).add(event);
                    continue;
                }

                EventAnalysis analysis = analyzer.apply(event);
                Set<String> relevantPlanIds = analysis.candidateInputs().stream()
                        .map(CandidateInputRef::planId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                for (String planId : relevantPlanIds) {
                    relevantAnalysesByPlanId.computeIfAbsent(planId, ignored -> new ArrayList<>()).add(analysis);
                }
            }
            return new JoinIndex(freezeEventMap(exactEventsByInputId), freezeAnalysisMap(relevantAnalysesByPlanId), !events.isEmpty());
        }

        private JoinIndex {
            exactEventsByInputId = exactEventsByInputId == null ? Map.of() : exactEventsByInputId;
            relevantAnalysesByPlanId = relevantAnalysesByPlanId == null ? Map.of() : relevantAnalysesByPlanId;
        }

        private List<DynamicEvidenceEvent> exactEvents(String inputVariantId) {
            return exactEventsByInputId.getOrDefault(inputVariantId, List.of());
        }

        private List<EventAnalysis> relevantAnalyses(String planId) {
            return relevantAnalysesByPlanId.getOrDefault(planId, List.of());
        }

        private boolean isEmpty() {
            return !hasEvents;
        }

        private static Map<String, List<DynamicEvidenceEvent>> freezeEventMap(Map<String, List<DynamicEvidenceEvent>> mutable) {
            Map<String, List<DynamicEvidenceEvent>> frozen = new LinkedHashMap<>();
            mutable.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
            return Collections.unmodifiableMap(frozen);
        }

        private static Map<String, List<EventAnalysis>> freezeAnalysisMap(Map<String, List<EventAnalysis>> mutable) {
            Map<String, List<EventAnalysis>> frozen = new LinkedHashMap<>();
            mutable.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
            return Collections.unmodifiableMap(frozen);
        }
    }

    private static final Comparator<DynamicEvidenceEvent> EVENT_ORDER = Comparator
            .comparing((DynamicEvidenceEvent event) -> event.sourcePath() == null ? "" : event.sourcePath().toString())
            .thenComparingInt(DynamicEvidenceEvent::lineNumber)
            .thenComparing(event -> event.eventId() == null ? "" : event.eventId());
}
