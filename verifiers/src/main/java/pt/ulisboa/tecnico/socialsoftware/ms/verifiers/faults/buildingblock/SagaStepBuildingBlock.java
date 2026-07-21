package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SagaStepBuildingBlock extends BuildingBlock {
    private final Set<String> predecessorStepKeys = new LinkedHashSet<>();
    private final List<StepDispatchFootprint> dispatches = new ArrayList<>();
    private final List<StepAnalysisDiagnostic> analysisDiagnostics = new ArrayList<>();
    private boolean compensationRegistered;
    private boolean forwardDispatchAnalysisComplete = true;
    private boolean compensationDispatchAnalysisComplete = true;
    private final String name;

    public SagaStepBuildingBlock(Path file, String packageName, String fqn, String name) {
        super(file, packageName, fqn);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPredecessorStepKeys() {
        return predecessorStepKeys;
    }

    public List<StepDispatchFootprint> getDispatches() {
        return dispatches;
    }

    public List<StepAnalysisDiagnostic> getAnalysisDiagnostics() {
        return analysisDiagnostics;
    }

    public boolean isCompensationRegistered() {
        return compensationRegistered;
    }

    public boolean isDispatchAnalysisComplete(DispatchPhase phase) {
        return phase == DispatchPhase.FORWARD
                ? forwardDispatchAnalysisComplete
                : compensationDispatchAnalysisComplete;
    }

    public void addPredecessorStepKey(String predecessorStepKey) {
        predecessorStepKeys.add(predecessorStepKey);
    }

    public void addDispatch(StepDispatchFootprint dispatch) {
        dispatches.add(dispatch);
    }

    public void markCompensationRegistered() {
        compensationRegistered = true;
    }

    public void markDispatchAnalysisIncomplete(DispatchPhase phase, String code, String message) {
        if (phase == DispatchPhase.FORWARD) {
            forwardDispatchAnalysisComplete = false;
        } else {
            compensationDispatchAnalysisComplete = false;
        }
        analysisDiagnostics.add(new StepAnalysisDiagnostic(phase, code, message));
    }
}
