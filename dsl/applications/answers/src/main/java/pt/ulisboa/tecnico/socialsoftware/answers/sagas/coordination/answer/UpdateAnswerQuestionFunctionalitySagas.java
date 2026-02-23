package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private AnswerQuestionDto updatedQuestionDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateAnswerQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerId, questionAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateQuestionStep = new SagaSyncStep("updateQuestionStep", () -> {
            AnswerQuestionDto updatedQuestionDto = answerService.updateAnswerQuestion(answerId, questionAggregateId, questionDto, unitOfWork);
            setUpdatedQuestionDto(updatedQuestionDto);
        });

        workflow.addStep(updateQuestionStep);
    }
    public AnswerQuestionDto getUpdatedQuestionDto() {
        return updatedQuestionDto;
    }

    public void setUpdatedQuestionDto(AnswerQuestionDto updatedQuestionDto) {
        this.updatedQuestionDto = updatedQuestionDto;
    }
}
