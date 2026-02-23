package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private OptionDto optionDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetQuestionOptionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, Integer key) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, key, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getOptionStep = new SagaSyncStep("getOptionStep", () -> {
            OptionDto optionDto = questionService.getOption(questionId, key, unitOfWork);
            setOptionDto(optionDto);
        });

        workflow.addStep(getOptionStep);
    }
    public OptionDto getOptionDto() {
        return optionDto;
    }

    public void setOptionDto(OptionDto optionDto) {
        this.optionDto = optionDto;
    }
}
