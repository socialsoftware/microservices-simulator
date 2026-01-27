package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllQuizsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizDto> quizs;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllQuizsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllQuizsStep = new SagaSyncStep("getAllQuizsStep", () -> {
            List<QuizDto> quizs = quizService.getAllQuizs();
            setQuizs(quizs);
        });

        workflow.addStep(getAllQuizsStep);

    }

    public List<QuizDto> getQuizs() {
        return quizs;
    }

    public void setQuizs(List<QuizDto> quizs) {
        this.quizs = quizs;
    }
}
