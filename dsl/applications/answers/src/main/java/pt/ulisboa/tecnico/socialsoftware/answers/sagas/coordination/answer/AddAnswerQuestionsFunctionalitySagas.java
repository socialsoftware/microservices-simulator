package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddAnswerQuestionsFunctionalitySagas extends WorkflowFunctionality {
    private List<AnswerQuestionDto> addedQuestionDtos;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddAnswerQuestionsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Integer answerId, List<AnswerQuestionDto> questionDtos) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(answerId, questionDtos, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, List<AnswerQuestionDto> questionDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addQuestionsStep = new SagaSyncStep("addQuestionsStep", () -> {
            List<AnswerQuestionDto> addedQuestionDtos = answerService.addAnswerQuestions(answerId, questionDtos, unitOfWork);
            setAddedQuestionDtos(addedQuestionDtos);
        });

        workflow.addStep(addQuestionsStep);
    }
    public List<AnswerQuestionDto> getAddedQuestionDtos() {
        return addedQuestionDtos;
    }

    public void setAddedQuestionDtos(List<AnswerQuestionDto> addedQuestionDtos) {
        this.addedQuestionDtos = addedQuestionDtos;
    }
}
