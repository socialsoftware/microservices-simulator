package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto updatedQuestionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, QuestionDto questionDto) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateQuestionStep = new SagaSyncStep("updateQuestionStep", () -> {
            QuestionDto updatedQuestionDto = questionService.updateQuestion(questionDto);
            setUpdatedQuestionDto(updatedQuestionDto);
        });

        workflow.addStep(updateQuestionStep);

    }

    public QuestionDto getUpdatedQuestionDto() {
        return updatedQuestionDto;
    }

    public void setUpdatedQuestionDto(QuestionDto updatedQuestionDto) {
        this.updatedQuestionDto = updatedQuestionDto;
    }
}
