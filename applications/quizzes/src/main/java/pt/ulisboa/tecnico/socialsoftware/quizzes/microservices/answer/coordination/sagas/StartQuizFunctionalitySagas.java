package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states.QuizSagaState;

public class StartQuizFunctionalitySagas extends WorkflowFunctionality {
    
    private QuizDto quizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public StartQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                       Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand getQuizByIdCommand = new GetQuizByIdCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            getQuizByIdCommand.setSemanticLock(QuizSagaState.READ_QUIZ);
            QuizDto quizDto = (QuizDto) commandGateway.send(getQuizByIdCommand);
            this.setQuizDto(quizDto);
        });
    
//        getQuizStep.registerCompensation(() -> {
//            Command command = new Command(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
//            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
//            commandGateway.send(command);
//        }, unitOfWork);
//
//        SagaSyncStep startQuizStep = new SagaSyncStep("startQuizStep", () -> {
//            StartQuizCommand startQuizCommand = new StartQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId, courseExecutionAggregateId, userAggregateId);
//            commandGateway.send(startQuizCommand);
//        }, new ArrayList<>(Arrays.asList(getQuizStep)));
    
        workflow.addStep(getQuizStep);
//        workflow.addStep(startQuizStep);
    }
    

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }
}