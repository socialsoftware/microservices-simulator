package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private OptionDto updatedOptionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateQuestionOptionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, Integer key, OptionDto optionDto) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, key, optionDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, OptionDto optionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateOptionStep = new SagaSyncStep("updateOptionStep", () -> {
            OptionDto updatedOptionDto = questionService.updateOption(questionId, key, optionDto, unitOfWork);
            setUpdatedOptionDto(updatedOptionDto);
        });

        workflow.addStep(updateOptionStep);
    }
    public OptionDto getUpdatedOptionDto() {
        return updatedOptionDto;
    }

    public void setUpdatedOptionDto(OptionDto updatedOptionDto) {
        this.updatedOptionDto = updatedOptionDto;
    }
}
