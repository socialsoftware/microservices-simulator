package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SagaFunctionalityBuildingBlock extends BuildingBlock {
    private final List<SagaStepBuildingBlock> steps = new ArrayList<>();

    public SagaFunctionalityBuildingBlock(Path file, String packageName, String fqn) {
        super(file, packageName, fqn);
    }

    public List<SagaStepBuildingBlock> getSteps() {
        return steps;
    }

    public void addStep(SagaStepBuildingBlock step) {
        steps.add(step);
    }
}
