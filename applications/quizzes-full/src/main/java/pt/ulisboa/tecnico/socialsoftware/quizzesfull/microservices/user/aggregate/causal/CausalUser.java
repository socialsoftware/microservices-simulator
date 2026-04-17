package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.causal;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.User;

import java.util.Set;

@Entity
public class CausalUser extends User implements CausalAggregate {

    public CausalUser() {
        super();
    }

    public CausalUser(User other) {
        super(other);
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of();
    }

    @Override
    public Set<String[]> getIntentions() {
        return Set.of();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitChangedFields,
                                Aggregate committedVersion,
                                Set<String> committedChangedFields) {
        return this;
    }
}
