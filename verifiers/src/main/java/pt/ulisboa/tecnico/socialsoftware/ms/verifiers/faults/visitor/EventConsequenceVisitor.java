package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandDispatchInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventConsequenceCandidate;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventDrivenFunctionalityInvocation;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventEmissionSiteFact;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util.TypeUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Extracts the first-slice direct producer emission and joins it to one selected consumer route. */
public final class EventConsequenceVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private final Map<String, ClassOrInterfaceDeclaration> classesByFqn = new LinkedHashMap<>();

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(declaration -> classFqn(declaration)
                .ifPresent(fqn -> classesByFqn.putIfAbsent(fqn, declaration)));
    }

    public void finish(ApplicationAnalysisState state) {
        state.eventConsequenceCandidates.clear();
        state.eventConsequenceDiagnostics.clear();

        Map<ServiceMethodKey, EmissionAnalysis> emissionsByServiceMethod = analyzeServiceMethods(state);
        Map<String, List<EventDrivenFunctionalityInvocation>> routesBySubscription = state.eventDrivenFunctionalityInvocations.stream()
                .sorted(routeOrder())
                .collect(java.util.stream.Collectors.groupingBy(
                        this::subscriptionKey,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        Set<String> emittingSagaFqns = emittingSagaFqns(state, emissionsByServiceMethod);
        LinkedHashSet<String> candidateKeys = new LinkedHashSet<>();
        for (SagaFunctionalityBuildingBlock saga : sortedSagas(state)) {
            for (SagaStepBuildingBlock step : saga.getSteps()) {
                String triggerStepKey = saga.getFqn() + "::" + step.getName();
                List<ResolvedStepEmission> stepEmissions = resolvedStepEmissions(
                        state, step, emissionsByServiceMethod);
                if (stepEmissions.isEmpty()) {
                    continue;
                }
                ResolvedStepEmission compensationOrigin = stepEmissions.stream()
                        .filter(emission -> "COMPENSATION_ORIGIN_EVENT_UNSUPPORTED".equals(
                                emission.analysis().diagnosticCode()))
                        .findFirst()
                        .orElse(null);
                if (compensationOrigin != null) {
                    diagnostic(state, "COMPENSATION_ORIGIN_EVENT_UNSUPPORTED:" + triggerStepKey);
                    continue;
                }
                if (stepEmissions.size() != 1) {
                    diagnostic(state, "MULTIPLE_EMISSIONS_UNSUPPORTED:" + triggerStepKey);
                    continue;
                }

                ResolvedStepEmission resolved = stepEmissions.get(0);
                if (!resolved.analysis().supported()) {
                    diagnostic(state, resolved.analysis().diagnosticCode() + ":" + triggerStepKey);
                    continue;
                }

                List<EventDrivenFunctionalityInvocation> matchingRoutes = state.eventDrivenFunctionalityInvocations.stream()
                        .filter(route -> Objects.equals(route.eventTypeFqn(), resolved.analysis().eventTypeFqn()))
                        .sorted(routeOrder())
                        .toList();
                if (matchingRoutes.isEmpty()) {
                    diagnostic(state, "NO_CONSUMER_ROUTE:" + triggerStepKey + ":" + resolved.analysis().eventTypeFqn());
                    continue;
                }

                for (EventDrivenFunctionalityInvocation route : matchingRoutes) {
                    List<EventDrivenFunctionalityInvocation> subscriptionRoutes = routesBySubscription.getOrDefault(
                            subscriptionKey(route), List.of());
                    if (subscriptionRoutes.size() != 1) {
                        diagnostic(state, "AMBIGUOUS_CONSUMER_ROUTE:" + subscriptionKey(route));
                        continue;
                    }
                    if (!state.hasSagaFqn(route.sagaClassFqn())) {
                        diagnostic(state, "NON_SAGA_DOWNSTREAM_UNSUPPORTED:" + subscriptionKey(route));
                        continue;
                    }
                    if (Objects.equals(saga.getFqn(), route.sagaClassFqn())
                            || emittingSagaFqns.contains(route.sagaClassFqn())) {
                        diagnostic(state, "RECURSIVE_EVENT_ROUTE_UNSUPPORTED:" + subscriptionKey(route));
                        continue;
                    }

                    EventEmissionSiteFact site = new EventEmissionSiteFact(
                            resolved.serviceMethodKey().serviceClassFqn(),
                            resolved.serviceMethodKey().methodSignature(),
                            resolved.analysis().emissionOrdinal(),
                            resolved.analysis().eventTypeFqn(),
                            List.of("direct registerEvent(new " + resolved.analysis().eventTypeFqn() + "(...), unitOfWork)",
                                    "command dispatch " + resolved.commandTypeFqn()));
                    EventConsequenceCandidate candidate = new EventConsequenceCandidate(
                            saga.getFqn(), triggerStepKey, site, route, List.of());
                    String key = candidateKey(candidate);
                    if (candidateKeys.add(key)) {
                        state.eventConsequenceCandidates.add(candidate);
                    }
                }
            }
        }

        state.eventConsequenceCandidates.sort(Comparator
                .comparing(EventConsequenceCandidate::triggerSagaFqn)
                .thenComparing(EventConsequenceCandidate::triggerStepKey)
                .thenComparing(candidate -> candidate.emissionSite().eventTypeFqn())
                .thenComparing(candidate -> candidate.selectedConsumerRoute().eventHandlingClassFqn())
                .thenComparing(candidate -> candidate.selectedConsumerRoute().eventHandlingMethodName()));
        state.eventConsequenceDiagnostics.sort(String::compareTo);
    }

    private Map<ServiceMethodKey, EmissionAnalysis> analyzeServiceMethods(ApplicationAnalysisState state) {
        Map<ServiceMethodKey, EmissionAnalysis> analyses = new LinkedHashMap<>();
        Set<String> serviceFqns = state.services.stream()
                .map(service -> service.getFqn())
                .collect(java.util.stream.Collectors.toSet());
        serviceFqns.stream().sorted().forEach(serviceFqn -> {
            ClassOrInterfaceDeclaration declaration = classesByFqn.get(serviceFqn);
            if (declaration == null) {
                return;
            }
            declaration.getMethods().stream()
                    .sorted(Comparator.comparing(TypeUtils::buildSignature))
                    .forEach(method -> {
                        List<MethodCallExpr> calls = method.findAll(MethodCallExpr.class).stream()
                                .filter(call -> "registerEvent".equals(call.getNameAsString()))
                                .filter(call -> call.findAncestor(MethodDeclaration.class).orElse(null) == method)
                                .sorted(Comparator.comparing(call -> call.getBegin().map(Object::toString).orElse(call.toString())))
                                .toList();
                        if (calls.isEmpty()) {
                            return;
                        }
                        ServiceMethodKey key = new ServiceMethodKey(serviceFqn, TypeUtils.buildSignature(method));
                        if (calls.size() != 1) {
                            analyses.put(key, EmissionAnalysis.rejected("MULTIPLE_EMISSIONS_UNSUPPORTED"));
                            return;
                        }
                        MethodCallExpr call = calls.get(0);
                        if (call.getArguments().size() != 2) {
                            analyses.put(key, EmissionAnalysis.rejected("UNRESOLVED_EMISSION_UNSUPPORTED"));
                            return;
                        }
                        if (!isSupportedUnitOfWorkReceiver(call)) {
                            analyses.put(key, EmissionAnalysis.rejected("UNSUPPORTED_EVENT_RECEIVER"));
                            return;
                        }
                        if (!isRelevantUnitOfWorkArgument(call.getArgument(1), method)) {
                            analyses.put(key, EmissionAnalysis.rejected(
                                    "UNSUPPORTED_EVENT_UNIT_OF_WORK_ARGUMENT"));
                            return;
                        }
                        if (isConditionalOrRepeated(call, method)) {
                            analyses.put(key, EmissionAnalysis.rejected("CONDITIONAL_EMISSION_UNSUPPORTED"));
                            return;
                        }
                        if (!call.getArgument(0).isObjectCreationExpr()) {
                            analyses.put(key, EmissionAnalysis.rejected("UNRESOLVED_EMISSION_UNSUPPORTED"));
                            return;
                        }
                        ObjectCreationExpr creation = call.getArgument(0).asObjectCreationExpr();
                        String eventTypeFqn;
                        try {
                            if (!TypeUtils.isResolvedSubtypeOf(creation.getType().resolve(), Event.class)) {
                                analyses.put(key, EmissionAnalysis.rejected("UNRESOLVED_EMISSION_UNSUPPORTED"));
                                return;
                            }
                            eventTypeFqn = creation.getType().resolve().describe();
                        } catch (RuntimeException failure) {
                            analyses.put(key, EmissionAnalysis.rejected("UNRESOLVED_EMISSION_UNSUPPORTED"));
                            return;
                        }
                        analyses.put(key, EmissionAnalysis.supported(eventTypeFqn, 0));
                    });
        });
        return analyses;
    }

    private List<ResolvedStepEmission> resolvedStepEmissions(ApplicationAnalysisState state,
                                                               SagaStepBuildingBlock step,
                                                               Map<ServiceMethodKey, EmissionAnalysis> analyses) {
        List<ResolvedStepEmission> resolved = new ArrayList<>();
        for (StepDispatchFootprint dispatch : step.getDispatches()) {
            List<CommandDispatchInfo> commandDispatches = commandDispatches(state, dispatch.commandTypeFqn());
            if (commandDispatches.isEmpty()) {
                continue;
            }
            if (commandDispatches.size() != 1) {
                resolved.add(new ResolvedStepEmission(
                        new ServiceMethodKey("(ambiguous)", "(ambiguous)"), dispatch.commandTypeFqn(),
                        EmissionAnalysis.rejected("AMBIGUOUS_PRODUCER_ROUTE_UNSUPPORTED")));
                continue;
            }
            CommandDispatchInfo commandDispatch = commandDispatches.get(0);
            ServiceMethodKey key = new ServiceMethodKey(
                    commandDispatch.serviceClassName(), commandDispatch.serviceMethodSignature());
            EmissionAnalysis analysis = analyses.get(key);
            if (analysis == null) {
                continue;
            }
            if (dispatch.phase() == DispatchPhase.COMPENSATION) {
                resolved.add(new ResolvedStepEmission(key, dispatch.commandTypeFqn(),
                        EmissionAnalysis.rejected("COMPENSATION_ORIGIN_EVENT_UNSUPPORTED")));
                continue;
            }
            if (dispatch.multiplicity() == null
                    || dispatch.multiplicity().kind() != DispatchMultiplicityKind.SINGLE) {
                resolved.add(new ResolvedStepEmission(key, dispatch.commandTypeFqn(),
                        EmissionAnalysis.rejected("MULTIPLE_EMISSIONS_UNSUPPORTED")));
                continue;
            }
            resolved.add(new ResolvedStepEmission(key, dispatch.commandTypeFqn(), analysis));
        }
        return resolved;
    }

    private Set<String> emittingSagaFqns(ApplicationAnalysisState state,
                                          Map<ServiceMethodKey, EmissionAnalysis> analyses) {
        Set<String> result = new LinkedHashSet<>();
        for (SagaFunctionalityBuildingBlock saga : sortedSagas(state)) {
            boolean emits = saga.getSteps().stream()
                    .flatMap(step -> step.getDispatches().stream())
                    .filter(dispatch -> dispatch.phase() == DispatchPhase.FORWARD)
                    .map(dispatch -> commandDispatches(state, dispatch.commandTypeFqn()))
                    .filter(dispatches -> dispatches.size() == 1)
                    .map(dispatches -> dispatches.get(0))
                    .map(dispatch -> new ServiceMethodKey(dispatch.serviceClassName(), dispatch.serviceMethodSignature()))
                    .map(analyses::get)
                    .anyMatch(Objects::nonNull);
            if (emits) {
                result.add(saga.getFqn());
            }
        }
        return result;
    }

    private List<CommandDispatchInfo> commandDispatches(ApplicationAnalysisState state, String commandTypeFqn) {
        Map<ServiceMethodKey, CommandDispatchInfo> distinct = new LinkedHashMap<>();
        state.commandHandlers.stream()
                .map(handler -> handler.getCommandDispatch().get(commandTypeFqn))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CommandDispatchInfo::serviceClassName)
                        .thenComparing(CommandDispatchInfo::serviceMethodSignature))
                .forEach(dispatch -> distinct.putIfAbsent(
                        new ServiceMethodKey(dispatch.serviceClassName(), dispatch.serviceMethodSignature()), dispatch));
        return List.copyOf(distinct.values());
    }

    private boolean isSupportedUnitOfWorkReceiver(MethodCallExpr call) {
        Expression scope = call.getScope().orElse(null);
        if (!(scope instanceof NameExpr) && !(scope instanceof FieldAccessExpr)) {
            return false;
        }
        try {
            return TypeUtils.isResolvedSubtypeOf(scope.calculateResolvedType(), UnitOfWorkService.class);
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private boolean isRelevantUnitOfWorkArgument(Expression argument, MethodDeclaration owner) {
        if (!(argument instanceof NameExpr name)) {
            return false;
        }
        List<com.github.javaparser.ast.body.Parameter> unitOfWorkParameters = owner.getParameters().stream()
                .filter(parameter -> TypeUtils.isSubtypeOf(parameter.getType(), UnitOfWork.class))
                .toList();
        return unitOfWorkParameters.size() == 1
                && Objects.equals(unitOfWorkParameters.get(0).getNameAsString(), name.getNameAsString());
    }

    private boolean isConditionalOrRepeated(MethodCallExpr call, MethodDeclaration owner) {
        Node current = call;
        while (current != owner && current.getParentNode().isPresent()) {
            current = current.getParentNode().orElseThrow();
            if (current instanceof IfStmt
                    || current instanceof SwitchEntry
                    || current instanceof ConditionalExpr
                    || current instanceof ForStmt
                    || current instanceof ForEachStmt
                    || current instanceof WhileStmt
                    || current instanceof DoStmt) {
                return true;
            }
        }
        return false;
    }

    private List<SagaFunctionalityBuildingBlock> sortedSagas(ApplicationAnalysisState state) {
        return state.sagas.stream().sorted(Comparator.comparing(SagaFunctionalityBuildingBlock::getFqn)).toList();
    }

    private Comparator<EventDrivenFunctionalityInvocation> routeOrder() {
        return Comparator.comparing(EventDrivenFunctionalityInvocation::eventTypeFqn)
                .thenComparing(EventDrivenFunctionalityInvocation::eventHandlingClassFqn)
                .thenComparing(EventDrivenFunctionalityInvocation::eventHandlingMethodName)
                .thenComparing(EventDrivenFunctionalityInvocation::eventHandlerClassFqn)
                .thenComparing(EventDrivenFunctionalityInvocation::eventProcessingClassFqn)
                .thenComparing(EventDrivenFunctionalityInvocation::eventProcessingMethodName)
                .thenComparing(EventDrivenFunctionalityInvocation::facadeClassFqn)
                .thenComparing(EventDrivenFunctionalityInvocation::facadeMethodName)
                .thenComparing(EventDrivenFunctionalityInvocation::sagaClassFqn);
    }

    private String subscriptionKey(EventDrivenFunctionalityInvocation route) {
        return String.join("|", route.eventHandlingClassFqn(), route.eventHandlingMethodName(),
                route.eventTypeFqn(), route.eventHandlerClassFqn());
    }

    private String candidateKey(EventConsequenceCandidate candidate) {
        return candidate.triggerStepKey() + "|" + candidate.emissionSite().sourceServiceClassFqn()
                + "|" + candidate.emissionSite().sourceServiceMethodSignature() + "|"
                + subscriptionKey(candidate.selectedConsumerRoute()) + "|"
                + candidate.selectedConsumerRoute().sagaClassFqn();
    }

    private void diagnostic(ApplicationAnalysisState state, String diagnostic) {
        if (!state.eventConsequenceDiagnostics.contains(diagnostic)) {
            state.eventConsequenceDiagnostics.add(diagnostic);
        }
    }

    private java.util.Optional<String> classFqn(ClassOrInterfaceDeclaration declaration) {
        try {
            return java.util.Optional.of(declaration.resolve().getQualifiedName());
        } catch (RuntimeException failure) {
            return declaration.getFullyQualifiedName();
        }
    }

    private record ServiceMethodKey(String serviceClassFqn, String methodSignature) {
    }

    private record ResolvedStepEmission(ServiceMethodKey serviceMethodKey,
                                        String commandTypeFqn,
                                        EmissionAnalysis analysis) {
    }

    private record EmissionAnalysis(boolean supported,
                                    String eventTypeFqn,
                                    int emissionOrdinal,
                                    String diagnosticCode) {
        private static EmissionAnalysis supported(String eventTypeFqn, int emissionOrdinal) {
            return new EmissionAnalysis(true, eventTypeFqn, emissionOrdinal, null);
        }

        private static EmissionAnalysis rejected(String code) {
            return new EmissionAnalysis(false, null, -1, code);
        }
    }
}
