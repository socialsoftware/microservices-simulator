package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddQuestionOptionsFunctionalitySagas extends WorkflowFunctionality {
    private List<OptionDto> addedOptionDtos;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddQuestionOptionsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, List<OptionDto> optionDtos) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, optionDtos, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, List<OptionDto> optionDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addOptionsStep = new SagaSyncStep("addOptionsStep", () -> {
            List<OptionDto> addedOptionDtos = questionService.addOptions(questionId, optionDtos, unitOfWork);
            setAddedOptionDtos(addedOptionDtos);
        });

        workflow.addStep(addOptionsStep);
    }
    public List<OptionDto> getAddedOptionDtos() {
        return addedOptionDtos;
    }

    public void setAddedOptionDtos(List<OptionDto> addedOptionDtos) {
        this.addedOptionDtos = addedOptionDtos;
    }
}
