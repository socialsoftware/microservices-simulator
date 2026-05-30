package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.FunctionalityUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public class FunctionalityStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final WorkflowFunctionality functionality;
    private final FlowStep step;
    private final Set<StepId> dependencies;

    private FunctionalityStep(
            FunctionalityId functionalityId,
            WorkflowFunctionality functionality,
            FlowStep step,
            Set<StepId> dependencies) {

        if (!FunctionalityUtils.getSteps(functionality).contains(step)) {
            throw new IllegalArgumentException("Step %s is not part of the given functionality of type [%s]"
                    .formatted(step.getName(), functionality.getClass().getName()));
        }

        // TODO when name is null should it instead default to index-naming? (step-1)
        String stepName = Objects.requireNonNull(step.getName(),
                () -> "Cannot create [%s] for functionality '%s' because step name is null"
                        .formatted(FunctionalityStep.class.getName(), functionalityId));

        id = StepId.forFunctionalityStep(functionalityId, stepName);
        this.functionalityId = functionalityId;
        this.functionality = functionality;
        this.step = step;
        this.dependencies = Set.copyOf(dependencies);
    }

    static List<FunctionalityStep> from(
            FunctionalityId functionalityId, WorkflowFunctionality functionality) {

        List<FunctionalityStep> steps = new ArrayList<>();
        Set<StepId> previousStepsIds = new HashSet<>();

        for (FlowStep step : FunctionalityUtils.getSteps(functionality)) {
            // ? TODO does it need to have all the previous steps as dependencies?
            // ? Or just the previous one? This also applies to other OracleStep classes
            var newStep = new FunctionalityStep(
                    functionalityId, functionality, step, previousStepsIds);

            steps.add(newStep);
            previousStepsIds.add(newStep.getId());
        }

        return steps;
    }

    @Override
    public void execute() {
        // TODO should change for a more precise method, e.g., func.executeStep(...) ?
        functionality.executeUntilStep(step.getName(), functionality.getWorkflow().getUnitOfWork());
    }

    @Override
    public StepId getId() {
        return id;
    }

    @Override
    public FunctionalityId getFunctionalityId() {
        return functionalityId;
    }

    @Override
    public Set<StepId> getDependencies() {
        return dependencies;
    }
}
