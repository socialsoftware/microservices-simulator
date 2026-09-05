package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public abstract class User extends Aggregate {
    private String name;
    private String username;
    @Enumerated(EnumType.STRING)
    private final Role role;
    private Boolean active;

    public User() {
        this.role = null;
    }

    public User(Integer aggregateId, String name, String username, Role role) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.username = username;
        this.role = role;
        this.active = false;
    }

    public User(User other) {
        super(other);
        this.name = other.getName();
        this.username = other.getUsername();
        this.role = other.getRole();
        this.active = other.isActive();
    }

    @Override
    public void verifyInvariants() {
        if (getState() == AggregateState.DELETED && Boolean.TRUE.equals(this.active)) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.USER_DELETED_STATE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
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

    public void setUsername(String username) {
        this.username = username;
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
}
