package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record EventDrivenFunctionalityInvocation(
        String eventHandlingClassFqn,
        String eventHandlingMethodName,
        String eventTypeFqn,
        String eventHandlerClassFqn,
        String eventProcessingClassFqn,
        String eventProcessingMethodName,
        String facadeClassFqn,
        String facadeMethodName,
        String sagaClassFqn,
        List<EventDrivenArgumentSource> argumentSources,
        List<String> resolutionNotes) {

    public EventDrivenFunctionalityInvocation {
        argumentSources = argumentSources == null ? List.of() : List.copyOf(argumentSources);
        resolutionNotes = resolutionNotes == null ? List.of() : List.copyOf(resolutionNotes);
    }
}
