package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final List<QuestionTopicDto> topics = new ArrayList<>();

    public UpdateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionAggregateId,
                                            String title, String content, List<Integer> topicAggregateIds,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, questionAggregateId, title, content, topicAggregateIds, unitOfWork,
                commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer questionAggregateId,
                              String title, String content, List<Integer> topicAggregateIds,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: re-seeds each QuestionTopic snapshot, including the courseAggregateId
        // that TOPIC_BELONGS_TO_QUESTION_COURSE is checked against at P1 in
        // Question.verifyInvariants().
        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            for (Integer topicAggregateId : topicAggregateIds) {
                GetTopicByIdCommand cmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
                TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
                this.topics.add(new QuestionTopicDto(topicDto.getAggregateId(), topicDto.getName(),
                        topicDto.getVersion(), topicDto.getCourseAggregateId()));
            }
        });

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand readCmd = new GetQuestionByIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(QuestionSagaState.IN_UPDATE_QUESTION);
            this.questionDto = (QuestionDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateQuestionStep = new SagaStep("updateQuestionStep", () -> {
            UpdateQuestionCommand cmd = new UpdateQuestionCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId,
                    title, content, this.topics);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTopicsStep, getQuestionStep)));

        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(getQuestionStep);
        this.workflow.addStep(updateQuestionStep);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }
}
