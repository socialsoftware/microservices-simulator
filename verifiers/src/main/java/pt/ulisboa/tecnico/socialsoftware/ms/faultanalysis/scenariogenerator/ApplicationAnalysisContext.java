package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator;

import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.CommandDispatchInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.CommandHandlerBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaCreationSite;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaInputSeed;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaWorkflowFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.ServiceBuildingBlock;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationAnalysisContext {
    public final Set<ServiceBuildingBlock> services = new LinkedHashSet<>();
    public final Set<CommandHandlerBuildingBlock> commandHandlers = new LinkedHashSet<>();
    public final Set<SagaWorkflowFunctionalityBuildingBlock> sagas = new LinkedHashSet<>();
    public final Set<SagaStepBuildingBlock> steps = new LinkedHashSet<>();
    public final Set<SagaInputSeed> inputSeeds = new LinkedHashSet<>();
    public final Set<SagaCreationSite> sagaCreationSites = new LinkedHashSet<>();

    public Optional<CommandDispatchInfo> resolveCommand(String commandType) {
        return commandHandlers.stream()
                .flatMap(h -> h.getCommandDispatch().entrySet().stream())
                .filter(e -> e.getKey().equals(commandType))
                .map(java.util.Map.Entry::getValue)
                .findFirst();
    }

    public Set<String> getSagaClassNames() {
        return sagas.stream()
                .map(SagaWorkflowFunctionalityBuildingBlock::getName)
                .collect(Collectors.toSet());
    }

    public Optional<String> resolveSagaCreation(String className, String methodName) {
        return sagaCreationSites.stream()
                .filter(s -> s.className().equals(className) && s.methodName().equals(methodName))
                .map(SagaCreationSite::sagaClassName)
                .findFirst();
    }
}
