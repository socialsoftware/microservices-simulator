package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.MappedSuperclass;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import java.util.Set;
import java.util.HashSet;

@MappedSuperclass
public abstract class Answer extends Aggregate {
    // Abstract base class for Answer aggregate
    // Concrete implementation is QuizAnswer
    
    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }
    
    @Override
    public void verifyInvariants() {
        // Invariant verification implementation
        // Override in concrete classes if needed
    }
}