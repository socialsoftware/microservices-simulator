package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.UpdateQuestionTopicsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateQuestionTopicsFunctionalityTCC extends WorkflowFunctionality {
    private Set<QuestionTopic> topics;
    private Question oldQuestion;
    private Set<QuestionTopic> oldTopics;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionTopicsFunctionalityTCC(QuestionFactory questionFactory, CausalUnitOfWorkService unitOfWorkService,
                                                Integer courseAggregateId, List<Integer> topicIds, CausalUnitOfWork unitOfWork,
                                                CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, topicIds, questionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, List<Integer> topicIds, QuestionFactory questionFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            Set<QuestionTopic> topics = topicIds.stream()
                    .map(id -> (TopicDto) commandGateway
                            .send(new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), id)))
                    .map(QuestionTopic::new)
                    .collect(Collectors.toSet());

            UpdateQuestionTopicsCommand cmd = new UpdateQuestionTopicsCommand(unitOfWork,
                    ServiceMapping.QUESTION.getServiceName(), courseAggregateId, topics);
            commandGateway.send(cmd);
        });

        workflow.addStep(step);
    }

    public Set<QuestionTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<QuestionTopic> topics) {
        this.topics = topics;
    }

    public Question getOldQuestion() {
        return oldQuestion;
    }

    public void setOldQuestion(Question oldQuestion) {
        this.oldQuestion = oldQuestion;
    }

    public Set<QuestionTopic> getOldTopics() {
        return oldTopics;
    }

    public void setOldTopics(Set<QuestionTopic> oldTopics) {
        this.oldTopics = oldTopics;
    }
}