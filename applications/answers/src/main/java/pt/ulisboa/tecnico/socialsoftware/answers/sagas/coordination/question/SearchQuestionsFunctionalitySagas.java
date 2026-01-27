package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class SearchQuestionsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionDto> searchedQuestionDtos;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public SearchQuestionsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, String title, String content) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(title, content, unitOfWork);
    }

    public void buildWorkflow(String title, String content, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchQuestionsStep = new SagaSyncStep("searchQuestionsStep", () -> {
            List<QuestionDto> searchedQuestionDtos = questionService.searchQuestions(title, content, unitOfWork);
            setSearchedQuestionDtos(searchedQuestionDtos);
        });

        workflow.addStep(searchQuestionsStep);

    }

    public List<QuestionDto> getSearchedQuestionDtos() {
        return searchedQuestionDtos;
    }

    public void setSearchedQuestionDtos(List<QuestionDto> searchedQuestionDtos) {
        this.searchedQuestionDtos = searchedQuestionDtos;
    }
}
