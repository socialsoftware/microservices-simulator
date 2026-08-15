package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement;

import java.util.List;

public interface ScenarioPrerequisiteProvider {
    String providerId();

    String providerVersion();

    ScenarioPrerequisiteResult prepare(ScenarioRuntimeContext runtimeContext,
                                       List<BaselineBindingRequirement> requiredBindings);
}
