package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.functionalities.QuestionFunctionalities;

@Service
public class QuestionEventProcessing {

    @Autowired
    private QuestionFunctionalities questionFunctionalities;

    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent event) {
        questionFunctionalities.updateTopicNameInQuestionByEvent(aggregateId, event.getPublisherAggregateId(), event.getTopicName());
    }

    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent event) {
        questionFunctionalities.removeTopicFromQuestionByEvent(aggregateId, event.getPublisherAggregateId());
    }
}
