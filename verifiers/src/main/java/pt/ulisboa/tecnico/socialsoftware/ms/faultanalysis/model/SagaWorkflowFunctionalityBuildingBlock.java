package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SagaWorkflowFunctionalityBuildingBlock extends BuildingBlock {
    private final List<SagaStepBuildingBlock> steps = new ArrayList<>();

    public SagaWorkflowFunctionalityBuildingBlock(Path file, String packageName, String className) {
        super(file, packageName, className);
    }

    public List<SagaStepBuildingBlock> getSteps() {
        return steps;
    }

    public void addStep(SagaStepBuildingBlock step) {
        steps.add(step);
    }
}
