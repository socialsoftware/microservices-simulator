package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private AnswerQuestionDto addedQuestionDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddAnswerQuestionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerId, questionAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addQuestionStep = new SagaSyncStep("addQuestionStep", () -> {
            AnswerQuestionDto addedQuestionDto = answerService.addAnswerQuestion(answerId, questionAggregateId, questionDto, unitOfWork);
            setAddedQuestionDto(addedQuestionDto);
        });

        workflow.addStep(addQuestionStep);
    }
    public AnswerQuestionDto getAddedQuestionDto() {
        return addedQuestionDto;
    }

    public void setAddedQuestionDto(AnswerQuestionDto addedQuestionDto) {
        this.addedQuestionDto = addedQuestionDto;
    }
}
