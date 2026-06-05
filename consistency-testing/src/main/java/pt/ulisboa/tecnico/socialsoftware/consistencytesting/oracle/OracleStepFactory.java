package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

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

    static List<OracleStep> buildStepsForFunctionalityCompensation(
            FunctionalityId functionalityId, WorkflowFunctionality func, SagaUnitOfWorkService uowService) {

        List<OracleStep> compensationSteps = new ArrayList<>(CompensationStep.from(functionalityId, func));

        Set<StepId> abortDependencies = compensationSteps.stream()
                .map(OracleStep::getId)
                .collect(Collectors.toUnmodifiableSet());

        compensationSteps.add(new AbortStep(functionalityId, func, abortDependencies, uowService));

        return compensationSteps;
    }
}
