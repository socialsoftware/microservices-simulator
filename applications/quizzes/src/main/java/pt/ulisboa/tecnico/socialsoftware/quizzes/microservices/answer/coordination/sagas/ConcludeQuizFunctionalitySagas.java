package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.GetQuizAnswerDtoByQuizIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class ConcludeQuizFunctionalitySagas extends WorkflowFunctionality {

    private QuizAnswerDto quizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public ConcludeQuizFunctionalitySagas(QuizAnswerService quizAnswerService, SagaUnitOfWorkService unitOfWorkService,
            Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
            CommandGateway CommandGateway) {
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuizAnswerStep = new SagaSyncStep("getQuizAnswerStep", () -> {
//            QuizAnswerDto quizAnswer = (QuizAnswerDto) quizAnswerService
//                    .getQuizAnswerDtoByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork); // TODO
//            unitOfWorkService.registerSagaState(quizAnswer.getAggregateId(), QuizAnswerSagaState.READ_QUIZ_ANSWER,
//                    unitOfWork);
            GetQuizAnswerDtoByQuizIdAndUserIdCommand getQuizAnswerDtoByQuizIdAndUserIdCommand = new GetQuizAnswerDtoByQuizIdAndUserIdCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAnswer.getAggregateId(), quizAggregateId, userAggregateId);
            getQuizAnswerDtoByQuizIdAndUserIdCommand.setSemanticLock(QuizAnswerSagaState.READ_QUIZ_ANSWER);
            CommandGateway.send(getQuizAnswerDtoByQuizIdAndUserIdCommand);
            this.setQuizAnswer(quizAnswer);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            // unitOfWorkService.registerSagaState(this.quizAnswer.getAggregateId(),
            // GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.ANSWER.getServiceName(),
                    this.quizAnswer.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            CommandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep concludeQuizStep = new SagaSyncStep("concludeQuizStep", () -> {
            quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork); // TODO
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