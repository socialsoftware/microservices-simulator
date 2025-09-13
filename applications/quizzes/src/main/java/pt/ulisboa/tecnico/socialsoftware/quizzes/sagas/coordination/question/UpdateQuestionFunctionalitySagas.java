package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuestionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto question;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionFunctionalitySagas(QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,
                                            QuestionFactory questionFactory, QuestionDto questionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionFactory, questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionFactory questionFactory, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
//            QuestionDto question = (QuestionDto) questionService.getQuestionById(questionDto.getAggregateId(), unitOfWork);
//            unitOfWorkService.registerSagaState(question.getAggregateId(), QuestionSagaState.READ_QUESTION, unitOfWork);
            GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto.getAggregateId());
            getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
            QuestionDto question = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
            this.setQuestion(question);
        });
    
        getQuestionStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(question.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), question.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep updateQuestionStep = new SagaSyncStep("updateQuestionStep", () -> {
//            questionService.updateQuestion(questionDto, unitOfWork);
            UpdateQuestionCommand updateQuestionCommand = new UpdateQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto);
            commandGateway.send(updateQuestionCommand);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));
    
        workflow.addStep(getQuestionStep);
        workflow.addStep(updateQuestionStep);
    }
    

    public QuestionDto getQuestion() {
        return question;
    }

    public void setQuestion(QuestionDto question) {
        this.question = question;
    }
}