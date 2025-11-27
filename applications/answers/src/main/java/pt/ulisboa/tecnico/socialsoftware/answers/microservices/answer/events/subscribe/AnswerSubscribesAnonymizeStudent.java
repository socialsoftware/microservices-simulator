package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.AnonymizeStudentEvent;

public class AnswerSubscribesAnonymizeStudent extends EventSubscription {
    public AnswerSubscribesAnonymizeStudent(AnswerUser answeruser) {
        super(answeruser.getStudentAggregateId(),
                answeruser.getStudentVersion(),
                AnonymizeStudentEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((AnonymizeStudentEvent)event).getGetStudentAggregateId()() == answerUser.getUserAggregateId();
    }
}
