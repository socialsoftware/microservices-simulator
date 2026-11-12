package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.functionalities.QuestionFunctionalities;

@Service
public class QuestionEventProcessing {

    @Autowired
    private QuestionFunctionalities questionFunctionalities;

    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent event) {
        questionFunctionalities.setTopicNameByEvent(aggregateId, event.getTopicAggregateId(),
                event.getTopicName(), event.getPublisherAggregateVersion());
    }

    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent event) {
        questionFunctionalities.removeDeletedTopicByEvent(aggregateId, event.getTopicAggregateId());
    }
}
