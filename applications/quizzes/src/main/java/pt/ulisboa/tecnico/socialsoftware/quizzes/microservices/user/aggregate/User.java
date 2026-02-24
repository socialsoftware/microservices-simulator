package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.INVARIANT_BREAK;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.USER_ACTIVE;

/*
    INTRA-INVARIANTS:
        ROLE IS FINAL
        DELETED_STATE
    INTER_INVARIANTS:
 */
@Entity
public abstract class User extends Aggregate {
    @Column
    private String name;
    @Column
    private String username;
    /*
        ROLE_FINAL
    */
    @Enumerated(EnumType.STRING)
    private final Role role;
    @Column(columnDefinition = "boolean default false")
    private Boolean active;

    public User() {
        this.role = null;
    }

    public User(User other) {
        super(other);
        setName(other.getName());
        setUsername(other.getUsername());
        this.role = other.getRole();
        setActive(other.isActive());
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(userDto.getName());
        setUsername(userDto.getUsername());
        this.role = Role.valueOf(userDto.getRole());
        setActive(false);
    }

    /*
        DELETED_STATE
     */
    public boolean deletedState() {
        if (getState() == AggregateState.DELETED) {
            return !isActive();
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (!(deletedState())) {
            throw new QuizzesException(INVARIANT_BREAK, getAggregateId());
        }
    }

    public void remove() {
        if (isActive()) {
            throw new QuizzesException(USER_ACTIVE, this.getAggregateId());
        }
        setState(AggregateState.DELETED);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public Role getRole() {
        return role;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }
}
