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

public class UpdateAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto updatedAnswerDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateAnswerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, AnswerDto answerDto) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerDto, unitOfWork);
    }

    public void buildWorkflow(AnswerDto answerDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAnswerStep = new SagaSyncStep("getAnswerStep", () -> {
            unitOfWorkService.registerSagaState(answerDto.getAggregateId(), AnswerSagaState.READ_ANSWER, unitOfWork);
        });

        getAnswerStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(answerDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep updateAnswerStep = new SagaSyncStep("updateAnswerStep", () -> {
            AnswerDto updatedAnswerDto = answerService.updateAnswer(answerDto.getAggregateId(), answerDto, unitOfWork);
            setUpdatedAnswerDto(updatedAnswerDto);
        }, new ArrayList<>(Arrays.asList(getAnswerStep)));

        workflow.addStep(getAnswerStep);
        workflow.addStep(updateAnswerStep);

    }

    public AnswerDto getUpdatedAnswerDto() {
        return updatedAnswerDto;
    }

    public void setUpdatedAnswerDto(AnswerDto updatedAnswerDto) {
        this.updatedAnswerDto = updatedAnswerDto;
    }
}
