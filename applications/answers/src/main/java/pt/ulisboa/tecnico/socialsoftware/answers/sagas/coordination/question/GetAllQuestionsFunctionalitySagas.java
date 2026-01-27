package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllQuestionsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllQuestionsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllQuestionsStep = new SagaSyncStep("getAllQuestionsStep", () -> {
            List<QuestionDto> questions = questionService.getAllQuestions();
            setQuestions(questions);
        });

        workflow.addStep(getAllQuestionsStep);

    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}
