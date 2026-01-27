package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetQuestionByIdFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetQuestionByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionAggregateId) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            QuestionDto questionDto = questionService.getQuestionById(questionAggregateId);
            setQuestionDto(questionDto);
        });

        workflow.addStep(getQuestionStep);

    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}
