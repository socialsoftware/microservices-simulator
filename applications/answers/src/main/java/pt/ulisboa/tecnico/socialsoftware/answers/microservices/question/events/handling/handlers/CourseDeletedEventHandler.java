package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.eventProcessing.QuestionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;

public class CourseDeletedEventHandler extends QuestionEventHandler {
    public CourseDeletedEventHandler(QuestionRepository questionRepository, QuestionEventProcessing questionEventProcessing) {
        super(questionRepository, questionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.questionEventProcessing.processCourseDeletedEvent(subscriberAggregateId, (CourseDeletedEvent) event);
    }
}
