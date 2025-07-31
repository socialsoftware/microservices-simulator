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
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.RemoveQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuestionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoveQuestionFunctionalitySagas extends WorkflowFunctionality {

    private SagaQuestionDto question;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveQuestionFunctionalitySagas(QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,
                                            Integer questionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
//            SagaQuestionDto question = (SagaQuestionDto) questionService.getQuestionById(questionAggregateId, unitOfWork);
//            unitOfWorkService.registerSagaState(questionAggregateId, QuestionSagaState.READ_QUESTION, unitOfWork);
            GetQuestionByIdCommand getQuestionByIdCommand = new  GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
            SagaQuestionDto question = (SagaQuestionDto) commandGateway.send(getQuestionByIdCommand);
            this.setQuestion(question);
        });
    
        getQuestionStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(questionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep removeQuestionStep = new SagaSyncStep("removeQuestionStep", () -> {
//            questionService.removeQuestion(questionAggregateId, unitOfWork);
            RemoveQuestionCommand removeQuestion = new RemoveQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            commandGateway.send(removeQuestion);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));
    
        workflow.addStep(getQuestionStep);
        workflow.addStep(removeQuestionStep);
    }
    public SagaQuestionDto getQuestion() {
        return question;
    }

    public void setQuestion(SagaQuestionDto question) {
        this.question = question;
    }
}