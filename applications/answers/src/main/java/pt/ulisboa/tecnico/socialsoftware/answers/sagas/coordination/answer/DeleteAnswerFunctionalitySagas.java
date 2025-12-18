package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.AnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class DeleteAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto deletedAnswerDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteAnswerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerAggregateId) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAnswerStep = new SagaSyncStep("getAnswerStep", () -> {
            AnswerDto deletedAnswerDto = answerService.getAnswerById(answerAggregateId, unitOfWork);
            setDeletedAnswerDto(deletedAnswerDto);
            unitOfWorkService.registerSagaState(answerAggregateId, AnswerSagaState.READ_ANSWER, unitOfWork);
        });

        getAnswerStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(answerAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep deleteAnswerStep = new SagaSyncStep("deleteAnswerStep", () -> {
            answerService.deleteAnswer(answerAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getAnswerStep)));

        workflow.addStep(getAnswerStep);
        workflow.addStep(deleteAnswerStep);

    }

    public AnswerDto getDeletedAnswerDto() {
        return deletedAnswerDto;
    }

    public void setDeletedAnswerDto(AnswerDto deletedAnswerDto) {
        this.deletedAnswerDto = deletedAnswerDto;
    }
}
