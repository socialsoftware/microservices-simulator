package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import com.github.javaparser.ast.type.Type;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationAnalysisState {
    public final List<ServiceBuildingBlock> services = new ArrayList<>();
    public final List<CommandHandlerBuildingBlock> commandHandlers = new ArrayList<>();
    public final List<SagaFunctionalityBuildingBlock> sagas = new ArrayList<>();
    public final List<WorkflowFunctionalityCreationSite> sagaCreationSites = new ArrayList<>();
    public final List<GroovyConstructorInputTrace> groovyConstructorInputTraces = new ArrayList<>();
    public final List<GroovyFullTraceResult> groovyFullTraceResults = new ArrayList<>();

    /**
     * Keyed by interface FQN → all @Service implementations found in the parsed source.
     * Populated by ServiceVisitor. Queried by CommandHandlerVisitor for interface-typed injection.
     * An entry with more than one value signals an ambiguous injection point (warn and skip).
     */
    public final Map<String, List<ServiceBuildingBlock>> interfaceToServices = new LinkedHashMap<>();

    /**
     * FQNs of concrete service types that are directly injected by at least one CommandHandler.
     * Populated by CommandHandlerIndexVisitor (Phase 1).
     * Used by ServiceVisitor (Phase 2) to restrict state.services to dispatch targets only.
     * Interface types are excluded — only concrete class FQNs are collected.
     */
    public final Set<String> dispatchTargetFqns = new LinkedHashSet<>();

    public final Optional<CommandDispatchInfo> getCommandDispatchInfo(Type commandType) {
        String commandTypeFqn;
        try {
            commandTypeFqn = commandType.resolve().describe();
        } catch (Exception e) {
            return Optional.empty();
        }

        return commandHandlers.stream()
                .map(CommandHandlerBuildingBlock::getCommandDispatch)
                .map(dispatches -> dispatches.get(commandTypeFqn))
                .filter(Objects::nonNull)
                .findFirst();
    }

    public Optional<SagaFunctionalityBuildingBlock> findSagaByFqn(String sagaFqn) {
        return sagas.stream()
                .filter(saga -> Objects.equals(saga.getFqn(), sagaFqn))
                .findFirst();
    }

    public boolean hasSagaFqn(String sagaFqn) {
        return findSagaByFqn(sagaFqn).isPresent();
    }

    public String formatHumanReadableReport() {
        StringBuilder report = new StringBuilder();

        appendLine(report, "Analysis Summary");
        appendLine(report, "=================");
        appendLine(report, "Dispatch targets (" + dispatchTargetFqns.size() + ")");
        appendIndentedEntries(report, dispatchTargetFqns.stream().sorted().toList(), 1);

        appendLine(report, "Services (" + services.size() + ")");
        if (services.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            services.stream()
                    .sorted(Comparator.comparing(ServiceBuildingBlock::getFqn))
                    .forEach(service -> {
                        appendLine(report, "- " + service.getFqn());
                        if (service.getMethodAccessPolicies().isEmpty()) {
                            appendLine(report, "  (no public methods)");
                            return;
                        }

                        service.getMethodAccessPolicies().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .forEach(entry ->
                                        appendLine(report, "  - " + entry.getKey() + " [" + entry.getValue() + "]"));
                    });
        }

        appendLine(report, "Interface service index (" + interfaceToServices.size() + ")");
        if (interfaceToServices.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            interfaceToServices.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> appendLine(report, "  - " + entry.getKey() + " -> " +
                            entry.getValue().stream()
                                    .map(ServiceBuildingBlock::getFqn)
                                    .sorted()
                                    .collect(Collectors.joining(", ", "[", "]"))));
        }

        appendLine(report, "Command handlers (" + commandHandlers.size() + ")");
        if (commandHandlers.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            commandHandlers.stream()
                    .sorted(Comparator.comparing(CommandHandlerBuildingBlock::getFqn))
                    .forEach(handler -> {
                        appendLine(report, "- " + handler.getFqn() +
                                (handler.getAggregateTypeName() == null ? "" : " [aggregate=" + handler.getAggregateTypeName() + "]"));

                        if (handler.getCommandDispatch().isEmpty()) {
                            appendLine(report, "  (no command dispatches)");
                            return;
                        }

                        handler.getCommandDispatch().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .forEach(entry -> {
                                    CommandDispatchInfo dispatch = entry.getValue();
                                    appendLine(report, "  - " + simpleName(entry.getKey()) +
                                            " -> " + simpleName(handler.getFqn()) +
                                            " / " + simpleName(dispatch.serviceClassName()) +
                                            "." + dispatch.serviceMethodSignature() +
                                            " [" + dispatch.accessPolicy() + "]");
                                });
                    });
        }

        appendLine(report, "Sagas (" + sagas.size() + ")");
        if (sagas.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            sagas.stream()
                    .sorted(Comparator.comparing(SagaFunctionalityBuildingBlock::getFqn))
                    .forEach(saga -> {
                        appendLine(report, "- " + saga.getFqn());
                        if (saga.getSteps().isEmpty()) {
                            appendLine(report, "  (no steps)");
                            return;
                        }

                        saga.getSteps().forEach(step -> appendSagaStep(report, step));
                    });
        }

        appendLine(report, "Saga creation sites (" + sagaCreationSites.size() + ")");
        if (sagaCreationSites.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            sagaCreationSites.stream()
                    .sorted(Comparator.comparing(WorkflowFunctionalityCreationSite::classFqn)
                            .thenComparing(WorkflowFunctionalityCreationSite::methodName)
                            .thenComparing(WorkflowFunctionalityCreationSite::sagaClassFqn))
                    .forEach(site -> appendLine(report, "- " +
                            simpleName(site.classFqn()) + "." + site.methodName() + "() -> " +
                            simpleName(site.sagaClassFqn())));
        }

        appendLine(report, "Groovy constructor-input traces (" + groovyConstructorInputTraces.size() + ")");
        if (groovyConstructorInputTraces.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            groovyConstructorInputTraces.stream()
                    .sorted(Comparator.comparing(GroovyConstructorInputTrace::sourceClassFqn)
                            .thenComparing(GroovyConstructorInputTrace::sourceMethodName)
                            .thenComparing(trace -> trace.sourceBindingName() == null ? "" : trace.sourceBindingName())
                            .thenComparing(GroovyConstructorInputTrace::sagaClassFqn))
                    .forEach(trace -> appendLine(report, "- " + formatGroovyTraceAnchor(trace.sourceClassFqn(),
                            trace.sourceMethodName(), trace.sourceBindingName(), trace.sagaClassFqn())));
        }

        appendLine(report, "Groovy full traces (" + groovyFullTraceResults.size() + ")");
        if (groovyFullTraceResults.isEmpty()) {
            appendLine(report, "  (none)");
        } else {
            groovyFullTraceResults.stream()
                    .sorted(Comparator.comparing(GroovyFullTraceResult::sourceClassFqn)
                            .thenComparing(GroovyFullTraceResult::sourceMethodName)
                            .thenComparing(trace -> trace.sourceBindingName() == null ? "" : trace.sourceBindingName())
                            .thenComparing(GroovyFullTraceResult::sagaClassFqn))
                    .forEach(trace -> {
                        appendLine(report, "- " + formatGroovyTraceAnchor(trace.sourceClassFqn(),
                                trace.sourceMethodName(), trace.sourceBindingName(), trace.sagaClassFqn()));

                        if (trace.traceText() != null && !trace.traceText().isBlank()) {
                            trace.traceText().lines()
                                    .forEach(line -> appendLine(report, "  " + line));
                        }
                    });
        }

        return report.toString().stripTrailing();
    }

    private void appendSagaStep(StringBuilder report, SagaStepBuildingBlock step) {
        appendLine(report, "  - " + step.getName());
        if (step.getPredecessorStepKeys().isEmpty()) {
            appendLine(report, "    predecessors: (none)");
        } else {
            appendLine(report, "    predecessors: " + String.join(", ", step.getPredecessorStepKeys()));
        }

        if (step.getDispatches().isEmpty()) {
            appendLine(report, "    dispatches: (none)");
            return;
        }

        step.getDispatches().forEach(dispatch -> appendLine(report, "    - " +
                simpleName(dispatch.commandTypeFqn()) + " -> " + dispatch.aggregateName() +
                " [" + dispatch.accessPolicy() + ", " + dispatch.phase() + ", " +
                formatMultiplicity(dispatch.multiplicity()) + "]"));
    }

    private static void appendIndentedEntries(StringBuilder report, List<String> entries, int indentLevel) {
        if (entries.isEmpty()) {
            appendLine(report, "  (none)");
            return;
        }

        String indent = "  ".repeat(indentLevel);
        entries.forEach(entry -> appendLine(report, indent + "- " + entry));
    }

    private static void appendLine(StringBuilder report, String line) {
        report.append(line).append(System.lineSeparator());
    }

    private static String formatMultiplicity(DispatchMultiplicity multiplicity) {
        if (multiplicity == null) {
            return "multiplicity=(unknown)";
        }

        if (multiplicity.staticCount() == null) {
            return multiplicity.kind().name();
        }

        return multiplicity.kind().name() + " x" + multiplicity.staticCount();
    }

    private static String simpleName(String fqn) {
        if (fqn == null || fqn.isBlank()) {
            return "(unknown)";
        }

        int lastDot = fqn.lastIndexOf('.');
        return lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
    }

    private static String formatGroovyTraceAnchor(String classFqn,
                                                  String methodName,
                                                  String sourceBindingName,
                                                  String sagaClassFqn) {
        String binding = sourceBindingName == null || sourceBindingName.isBlank()
                ? ""
                : " [binding=" + sourceBindingName + "]";
        return simpleName(classFqn) + "." + methodName + "()" + binding + " -> " + simpleName(sagaClassFqn);
    }
}
