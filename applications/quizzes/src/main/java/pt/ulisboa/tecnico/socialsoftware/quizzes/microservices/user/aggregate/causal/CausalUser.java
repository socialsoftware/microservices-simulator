package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.HashSet;
import java.util.Set;

@Entity
public class CausalUser extends User implements CausalAggregate {
    public CausalUser() {
        super();
    }

    public CausalUser(CausalUser other) {
        super(other);
    }

    public CausalUser(Integer aggregateId, UserDto userDto) {
        super(aggregateId, userDto);
    }

    @Override
    @JsonIgnore
    public Set<String> getMutableFields() {
        return Set.of("name", "username", "active");
    }

    @Override
    @JsonIgnore
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        return this;
    }
}
