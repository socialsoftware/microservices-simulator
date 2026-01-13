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


    public SearchAnswersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, Boolean completed) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(completed, unitOfWork);
    }

    public void buildWorkflow(Boolean completed, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchAnswersStep = new SagaSyncStep("searchAnswersStep", () -> {
            List<AnswerDto> searchedAnswerDtos = answerService.searchAnswers(completed, unitOfWork);
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
