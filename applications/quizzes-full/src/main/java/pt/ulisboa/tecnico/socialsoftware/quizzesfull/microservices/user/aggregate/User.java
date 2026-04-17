package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import static pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState.ACTIVE;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.USER_MISSING_ROLE;

/*
    INTRA-INVARIANTS:
        USER_KEY_FINAL (enforced by final field)
        USER_ROLE_FINAL (enforced by final field)
        USER_ROLE_NOT_NULL
        USER_DELETED_STATE
    INTER-INVARIANTS:
        (none — User is a root publisher)
*/
@Entity
public abstract class User extends Aggregate {

    /*
        USER_KEY_FINAL
    */
    @Column
    private final Integer key;

    @Column
    private String name;

    @Column
    private String username;

    /*
        USER_ROLE_FINAL
    */
    @Enumerated(EnumType.STRING)
    private final Role role;

    @Column
    private Boolean active;

    public User() {
        this.key = 0;
        this.name = "USER NAME";
        this.username = "USER USERNAME";
        this.role = Role.STUDENT;
        this.active = true;
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.key = userDto.getKey();
        this.name = userDto.getName();
        this.username = userDto.getUsername();
        this.role = userDto.getRole() != null ? Role.valueOf(userDto.getRole()) : null;
        this.active = userDto.getActive() != null ? userDto.getActive() : true;
    }

    public User(User other) {
        super(other);
        this.key = other.getKey();
        this.name = other.getName();
        this.username = other.getUsername();
        this.role = other.getRole();
        this.active = other.getActive();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    /*
     * USER_ROLE_NOT_NULL
     * User.role must not be null
     */
    private boolean invariantUserRoleNotNull() {
        return role != null;
    }

    /*
     * USER_DELETED_STATE
     * If User.state == DELETED then User.active must be false
     */
    private boolean invariantUserDeletedState() {
        if (getState() == AggregateState.DELETED) {
            return !active;
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (getState() == ACTIVE) {
            if (!invariantUserRoleNotNull()) {
                throw new QuizzesFullException(USER_MISSING_ROLE);
            }
        }
        if (!invariantUserDeletedState()) {
            throw new QuizzesFullException("User in DELETED state must have active = false");
        }
    }

    public Integer getKey() {
        return key;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
