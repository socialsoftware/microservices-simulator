package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class SearchAnswersFunctionalitySagas extends WorkflowFunctionality {
    private List<AnswerDto> searchedAnswerDtos;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public SearchAnswersFunctionalitySagas(AnswerService answerService, SagaUnitOfWorkService unitOfWorkService, Boolean completed, Integer executionAggregateId, Integer userAggregateId, Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(completed, executionAggregateId, userAggregateId, quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Boolean completed, Integer executionAggregateId, Integer userAggregateId, Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchAnswersStep = new SagaSyncStep("searchAnswersStep", () -> {
            List<AnswerDto> searchedAnswerDtos = answerService.searchAnswers(completed, executionAggregateId, userAggregateId, quizAggregateId, unitOfWork);
            setSearchedAnswerDtos(searchedAnswerDtos);
        });

        workflow.addStep(searchAnswersStep);
    }

    public List<AnswerDto> getSearchedAnswerDtos() {
        return searchedAnswerDtos;
    }

    public void setSearchedAnswerDtos(List<AnswerDto> searchedAnswerDtos) {
        this.searchedAnswerDtos = searchedAnswerDtos;
    }
}
