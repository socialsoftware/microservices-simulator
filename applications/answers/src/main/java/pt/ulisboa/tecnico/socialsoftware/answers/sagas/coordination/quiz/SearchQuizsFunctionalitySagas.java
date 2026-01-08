package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;
import java.util.List;

public class SearchQuizsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizDto> searchedQuizDtos;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public SearchQuizsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, String title, QuizType quizType, Integer executionAggregateId) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(title, quizType, executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(String title, QuizType quizType, Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchQuizsStep = new SagaSyncStep("searchQuizsStep", () -> {
            List<QuizDto> searchedQuizDtos = quizService.searchQuizs(title, quizType, executionAggregateId, unitOfWork);
            setSearchedQuizDtos(searchedQuizDtos);
        });

        workflow.addStep(searchQuizsStep);

    }

    public List<QuizDto> getSearchedQuizDtos() {
        return searchedQuizDtos;
    }

    public void setSearchedQuizDtos(List<QuizDto> searchedQuizDtos) {
        this.searchedQuizDtos = searchedQuizDtos;
    }
}
