package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;

public class UpdateQuestionTopicsFunctionalityTCC extends WorkflowFunctionality {
    private Set<QuestionTopic> topics;
    private Question oldQuestion;
    private Set<QuestionTopic> oldTopics;
    private final QuestionService questionService;
    private final TopicService topicService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public UpdateQuestionTopicsFunctionalityTCC(QuestionService questionService, TopicService topicService, 
                                QuestionFactory questionFactory, CausalUnitOfWorkService unitOfWorkService,  
                                Integer courseAggregateId, List<Integer> topicIds, CausalUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, topicIds, questionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, List<Integer> topicIds, QuestionFactory questionFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            Set<QuestionTopic> topics = topicIds.stream()
                        .map(id -> topicService.getTopicById(id, unitOfWork))
                        .map(QuestionTopic::new)
                        .collect(Collectors.toSet());

            questionService.updateQuestionTopics(courseAggregateId, topics, unitOfWork);
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