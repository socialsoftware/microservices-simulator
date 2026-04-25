package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;

import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        USER_ROLE_FINAL (final field)
        USER_DELETED_STATE
    INTER-INVARIANTS:
        (none)
 */
@Entity
public abstract class User extends Aggregate {

    @Column
    private String name;

    @Column
    private String username;

    /*
        USER_ROLE_FINAL
     */
    @Enumerated(EnumType.STRING)
    private final UserRole role;

    @Column(columnDefinition = "boolean default false")
    private Boolean active;

    public User() {
        this.role = null;
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(userDto.getName());
        setUsername(userDto.getUsername());
        this.role = UserRole.valueOf(userDto.getRole());
        setActive(false);
    }

    public User(User other) {
        super(other);
        setName(other.getName());
        setUsername(other.getUsername());
        this.role = other.getRole();
        setActive(other.isActive());
    }

    private boolean deletedState() {
        if (getState() == AggregateState.DELETED) {
            return !isActive();
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (!deletedState()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.USER_DELETED_STATE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public UserRole getRole() { return role; }

    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
