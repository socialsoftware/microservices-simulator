package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

final class OracleStepFactory {

    private OracleStepFactory() {
        throw new UnsupportedOperationException("Instantiation is not allowed");
    }

    static List<OracleStep> buildStepsForFunctionality(
            FunctionalityId functionalityId, WorkflowFunctionality func, SagaUnitOfWorkService uowService) {

        List<OracleStep> steps = new ArrayList<>(FunctionalityStep.from(functionalityId, func));

        Set<StepId> commitDependencies = steps.stream()
                .map(OracleStep::getId)
                .collect(Collectors.toUnmodifiableSet());

        steps.add(new CommitStep(functionalityId, func, commitDependencies, uowService));

        return steps;
    }

    /**
     * Builds the steps a functionality runs while aborting.
     * <p>
     * Mirrors {@code SagaUnitOfWorkService.abortUntilStep}: it walks the executed
     * steps in reverse and, for each one, runs that step's compensation and then
     * reverts the semantic locks that step acquired. Both halves are modelled as
     * steps of their own, so every point at which the system can be interleaved
     * while aborting is a point the oracle can schedule.
     * <p>
     * A step that failed mid-execution is recorded as executed but registers no
     * compensation, so none is run, and only aborts (gets its locks reverted).
     */
    static List<OracleStep> buildStepsForFunctionalityCompensation(
            FunctionalityId functionalityId, WorkflowFunctionality func, SagaUnitOfWorkService uowService) {

        UnitOfWork unitOfWork = func.getWorkflow().getUnitOfWork();
        if (!(unitOfWork instanceof SagaUnitOfWork sagaUow)) {
            throw new IllegalArgumentException(
                    "Cannot create compensation steps for a workflow with a unit of work of type %s expected type was %s"
                            .formatted(unitOfWork.getClass(), SagaUnitOfWork.class));
        }

        Set<String> registeredCompensationStepNames = Set.copyOf(sagaUow.getRegisteredCompensationStepNames());

        List<OracleStep> compensationSteps = new ArrayList<>();
        Set<StepId> previousStepIds = new HashSet<>();

        for (String stepName : sagaUow.getExecutedSteps().reversed()) {
            if (registeredCompensationStepNames.contains(stepName)) {
                var compensationStep = new CompensationStep(
                        functionalityId, sagaUow, stepName, previousStepIds);

                compensationSteps.add(compensationStep);
                previousStepIds.add(compensationStep.getId());
            }

            var abortStep = new AbortStep(
                    functionalityId, sagaUow, uowService, stepName, previousStepIds);

            compensationSteps.add(abortStep);
            previousStepIds.add(abortStep.getId());
        }

        return compensationSteps;
    }
}
