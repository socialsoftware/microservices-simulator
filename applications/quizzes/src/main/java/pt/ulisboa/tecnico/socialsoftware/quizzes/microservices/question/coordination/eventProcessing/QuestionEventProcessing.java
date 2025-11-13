package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;

@Service
public class QuestionEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(QuestionEventProcessing.class);

    @Autowired
    private QuestionFunctionalities questionFunctionalities;

    /************************************************
     * EVENT PROCESSING
     ************************************************/

    public void processUpdateTopic(Integer aggregateId, UpdateTopicEvent updateTopicEvent) {
        logger.info("Processing UpdateTopicEvent: aggregateId={}, event={}", aggregateId, updateTopicEvent);
        questionFunctionalities.updateTopicInQuestion(aggregateId, updateTopicEvent.getPublisherAggregateId(),
                updateTopicEvent.getTopicName(), updateTopicEvent.getPublisherAggregateVersion());
    }

    public void processDeleteTopic(Integer aggregateId, DeleteTopicEvent deleteTopicEvent) {
        logger.info("Processing DeleteTopicEvent: aggregateId={}, event={}", aggregateId, deleteTopicEvent);
        questionFunctionalities.deleteTopicInQuestion(aggregateId, deleteTopicEvent.getPublisherAggregateId(),
                deleteTopicEvent.getPublisherAggregateVersion());
    }
}
