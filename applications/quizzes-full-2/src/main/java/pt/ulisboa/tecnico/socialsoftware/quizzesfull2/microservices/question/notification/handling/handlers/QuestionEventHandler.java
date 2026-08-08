package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.handling.handlers;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.eventProcessing.QuestionEventProcessing;

@Component
public class QuestionEventHandler extends EventHandler {

    private final QuestionEventProcessing questionEventProcessing;

    public QuestionEventHandler(QuestionRepository questionRepository,
                                QuestionEventProcessing questionEventProcessing) {
        super(questionRepository);
        this.questionEventProcessing = questionEventProcessing;
    }

    @Override
    protected Class<? extends Aggregate> aggregateType() {
        return Question.class;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof UpdateTopicEvent e) {
            questionEventProcessing.processUpdateTopicEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteTopicEvent e) {
            questionEventProcessing.processDeleteTopicEvent(subscriberAggregateId, e);
        }
    }
}
