package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import com.github.javaparser.ast.expr.ObjectCreationExpr;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SagaStepBuildingBlock extends BuildingBlock {
    private final ObjectCreationExpr creationExpr;
    private final List<StepFootprint> stepFootprints = new ArrayList<>();

    public SagaStepBuildingBlock(Path file, String packageName, String name, ObjectCreationExpr creationExpr) {
        super(file, packageName, name);
        this.creationExpr = creationExpr;
    }

    public void addStepFootprint(StepFootprint stepFootprint) {
        stepFootprints.add(stepFootprint);
    }

    public List<StepFootprint> getStepFootprints() {
        return stepFootprints;
    }

    public ObjectCreationExpr getCreationExpr() {
        return creationExpr;
    }

    public void buildFootprints() {
    }
}
