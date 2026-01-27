package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllAnswersFunctionalitySagas extends WorkflowFunctionality {
    private List<AnswerDto> answers;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllAnswersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllAnswersStep = new SagaSyncStep("getAllAnswersStep", () -> {
            List<AnswerDto> answers = answerService.getAllAnswers();
            setAnswers(answers);
        });

        workflow.addStep(getAllAnswersStep);

    }

    public List<AnswerDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerDto> answers) {
        this.answers = answers;
    }
}
