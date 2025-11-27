package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UpdateStudentNameEvent;

public class AnswerSubscribesUpdateStudentName extends EventSubscription {
    public AnswerSubscribesUpdateStudentName(AnswerUser answeruser) {
        super(answeruser.getStudentAggregateId(),
                answeruser.getStudentVersion(),
                UpdateStudentNameEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((UpdateStudentNameEvent)event).getGetStudentAggregateId()() == answerUser.getUserAggregateId();
    }
}
