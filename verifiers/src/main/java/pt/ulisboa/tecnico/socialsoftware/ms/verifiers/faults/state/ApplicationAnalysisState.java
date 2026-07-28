package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import com.github.javaparser.ast.type.Type;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

public class ApplicationAnalysisState {
    public final List<ServiceBuildingBlock> services = new ArrayList<>();
    public final List<CommandHandlerBuildingBlock> commandHandlers = new ArrayList<>();
    public final List<SagaFunctionalityBuildingBlock> sagas = new ArrayList<>();
    public final List<WorkflowFunctionalityCreationSite> sagaCreationSites = new ArrayList<>();
    public final List<EventDrivenFunctionalityInvocation> eventDrivenFunctionalityInvocations = new ArrayList<>();
    public final List<String> eventDrivenFunctionalityDiagnostics = new ArrayList<>();
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
     */
    public final Set<String> dispatchTargetFqns = new LinkedHashSet<>();

    /**
     * FQNs of interface types injected by at least one CommandHandler. These are resolved later
     * to concrete services only when the application source has a single @Service implementation.
     */
    public final Set<String> dispatchTargetInterfaceFqns = new LinkedHashSet<>();

    /**
     * Counts @Service implementations per interface, populated during the index pass before
     * ServiceVisitor applies its dispatch-target guard.
     */
    public final Map<String, Integer> serviceImplementationCountsByInterface = new LinkedHashMap<>();

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
}
