package pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event;

import pt.ulisboa.tecnico.socialsoftware.blcm.question.domain.Question;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.EventType.REMOVE_QUESTION;

@Entity
@DiscriminatorValue(REMOVE_QUESTION)
public class RemoveQuestionEvent extends Event {

    public RemoveQuestionEvent() {
        super();
    }

    public RemoveQuestionEvent(Question question) {
        super(question.getAggregateId());
    }
}
