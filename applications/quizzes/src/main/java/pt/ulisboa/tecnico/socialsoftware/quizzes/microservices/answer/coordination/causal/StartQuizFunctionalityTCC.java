package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer.StartQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class StartQuizFunctionalityTCC extends WorkflowFunctionality {
    private QuizDto quizDto;
    private UserDto userDto;
    private QuizAnswerDto quizAnswerDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public StartQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                     Integer quizAggregateId, Integer courseExecutionAggregateId,
                                     Integer userAggregateId, CausalUnitOfWork unitOfWork,
                                     CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer courseExecutionAggregateId,
                              Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step("startQuizStep", () -> {
            GetQuizByIdCommand getQuizByIdCommand = new GetQuizByIdCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            QuizDto quizDto = (QuizDto) commandGateway.send(getQuizByIdCommand);
            this.setQuizDto(quizDto);

            GetUserByIdCommand getUserByIdCommand = new GetUserByIdCommand(unitOfWork,
                    ServiceMapping.USER.getServiceName(), userAggregateId);
            UserDto userDto = (UserDto) commandGateway.send(getUserByIdCommand);
            this.setUserDto(userDto);

            StartQuizCommand startQuizCommand = new StartQuizCommand(unitOfWork,
                    ServiceMapping.ANSWER.getServiceName(), quizAggregateId, courseExecutionAggregateId,
                    this.getQuizDto(), this.getUserDto());
            QuizAnswerDto quizAnswerDto = (QuizAnswerDto) commandGateway.send(startQuizCommand);
            this.setQuizAnswerDto(quizAnswerDto);
        });

        workflow.addStep(step);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }

    public void setQuizAnswerDto(QuizAnswerDto quizAnswerDto) {
        this.quizAnswerDto = quizAnswerDto;
    }
}
