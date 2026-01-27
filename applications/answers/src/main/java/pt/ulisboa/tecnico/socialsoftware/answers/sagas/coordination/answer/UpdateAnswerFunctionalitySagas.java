package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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

        SagaSyncStep updateAnswerStep = new SagaSyncStep("updateAnswerStep", () -> {
            AnswerDto updatedAnswerDto = answerService.updateAnswer(answerDto, unitOfWork);
            setUpdatedAnswerDto(updatedAnswerDto);
        });

        workflow.addStep(updateAnswerStep);

    }

    public AnswerDto getUpdatedAnswerDto() {
        return updatedAnswerDto;
    }

    public void setUpdatedAnswerDto(AnswerDto updatedAnswerDto) {
        this.updatedAnswerDto = updatedAnswerDto;
    }
}
