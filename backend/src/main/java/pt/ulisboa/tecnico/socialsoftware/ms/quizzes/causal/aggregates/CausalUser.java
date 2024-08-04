package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

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
}
