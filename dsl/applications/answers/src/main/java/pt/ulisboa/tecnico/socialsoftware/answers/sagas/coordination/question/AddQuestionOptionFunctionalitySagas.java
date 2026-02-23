package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private OptionDto addedOptionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddQuestionOptionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, Integer key, OptionDto optionDto) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, key, optionDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, OptionDto optionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addOptionStep = new SagaSyncStep("addOptionStep", () -> {
            OptionDto addedOptionDto = questionService.addOption(questionId, key, optionDto, unitOfWork);
            setAddedOptionDto(addedOptionDto);
        });

        workflow.addStep(addOptionStep);
    }
    public OptionDto getAddedOptionDto() {
        return addedOptionDto;
    }

    public void setAddedOptionDto(OptionDto addedOptionDto) {
        this.addedOptionDto = addedOptionDto;
    }
}
