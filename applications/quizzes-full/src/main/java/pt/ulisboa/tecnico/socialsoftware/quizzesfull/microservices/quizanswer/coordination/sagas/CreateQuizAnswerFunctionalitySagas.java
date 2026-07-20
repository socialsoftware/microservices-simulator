package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.CreateQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.states.UserSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateQuizAnswerFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private UserDto userDto;
    private QuizAnswerDto createdQuizAnswerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuizAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer quizId, Integer userId,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizId, userId, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, Integer userId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand getCmd = new GetQuizByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(QuizSagaState.READ_QUIZ);
            this.quizDto = (QuizDto) commandGateway.send(sagaCmd);
        });

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand getCmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(UserSagaState.READ_USER);
            this.userDto = (UserDto) commandGateway.send(sagaCmd);
        }, new ArrayList<>(Arrays.asList(getQuizStep)));

        SagaStep createQuizAnswerStep = new SagaStep("createQuizAnswerStep", () -> {
            CreateQuizAnswerCommand cmd = new CreateQuizAnswerCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(),
                    this.quizDto.getAggregateId(), this.quizDto.getVersion(),
                    this.userDto.getAggregateId(), this.userDto.getVersion(),
                    this.userDto.getName(), this.userDto.getUsername(),
                    this.quizDto.getExecutionId(), this.quizDto.getExecutionVersion());
            this.createdQuizAnswerDto = (QuizAnswerDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getQuizStep);
        this.workflow.addStep(getUserStep);
        this.workflow.addStep(createQuizAnswerStep);
    }

    public QuizAnswerDto getCreatedQuizAnswerDto() { return createdQuizAnswerDto; }
}
