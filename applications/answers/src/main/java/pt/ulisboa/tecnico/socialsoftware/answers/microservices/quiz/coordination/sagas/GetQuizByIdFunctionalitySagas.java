package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.states.QuizSagaState;

public class GetQuizByIdFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetQuizByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            unitOfWorkService.verifySagaState(quizAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(QuizSagaState.UPDATE_QUIZ, QuizSagaState.DELETE_QUIZ)));
            unitOfWorkService.registerSagaState(quizAggregateId, QuizSagaState.READ_QUIZ, unitOfWork);
            GetQuizByIdCommand cmd = new GetQuizByIdCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            QuizDto quizDto = (QuizDto) commandGateway.send(cmd);
            setQuizDto(quizDto);
        });

        workflow.addStep(getQuizStep);
    }
    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }
}
