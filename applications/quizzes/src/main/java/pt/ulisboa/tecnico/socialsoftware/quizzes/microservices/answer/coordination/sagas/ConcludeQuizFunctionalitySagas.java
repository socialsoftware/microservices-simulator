package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.GetQuizAnswerDtoByQuizIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class ConcludeQuizFunctionalitySagas extends WorkflowFunctionality {

    private QuizAnswerDto quizAnswer;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public ConcludeQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
                                          CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerDtoByQuizIdAndUserIdCommand getQuizAnswerDtoByQuizIdAndUserIdCommand = new GetQuizAnswerDtoByQuizIdAndUserIdCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAnswer.getAggregateId(), quizAggregateId, userAggregateId);
            getQuizAnswerDtoByQuizIdAndUserIdCommand.setSemanticLock(QuizAnswerSagaState.READ_QUIZ_ANSWER);
            commandGateway.send(getQuizAnswerDtoByQuizIdAndUserIdCommand);
            this.setQuizAnswer(quizAnswer);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.ANSWER.getServiceName(), this.quizAnswer.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep concludeQuizStep = new SagaStep("concludeQuizStep", () -> {
//            quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork); // TODO
            ConcludeQuizCommand concludeQuizCommand = new ConcludeQuizCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAggregateId, userAggregateId);
            commandGateway.send(concludeQuizCommand);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));

        workflow.addStep(getQuizAnswerStep);
        workflow.addStep(concludeQuizStep);
    }

    public QuizAnswerDto getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswerDto quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}