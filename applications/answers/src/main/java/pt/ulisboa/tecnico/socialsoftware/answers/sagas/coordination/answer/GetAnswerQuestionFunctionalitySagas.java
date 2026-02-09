package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private AnswerQuestionDto questionDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetAnswerQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerId, Integer questionAggregateId) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerId, questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            AnswerQuestionDto questionDto = answerService.getAnswerQuestion(answerId, questionAggregateId, unitOfWork);
            setQuestionDto(questionDto);
        });

        workflow.addStep(getQuestionStep);
    }
    public AnswerQuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(AnswerQuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}
