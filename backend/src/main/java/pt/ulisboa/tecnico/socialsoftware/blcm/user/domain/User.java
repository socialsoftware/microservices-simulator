package pt.ulisboa.tecnico.socialsoftware.blcm.user.domain;

import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.blcm.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.dto.UserDto;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate.AggregateState.DELETED;
import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.AggregateType.USER;
import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.EventType.REMOVE_COURSE_EXECUTION;
import static pt.ulisboa.tecnico.socialsoftware.blcm.exception.ErrorMessage.*;

/*
    INTRA-INVARIANTS:
        ROLE IS FINAL
        DELETED_STATE
    INTER_INVARIANTS:

 */
@Entity
@Table(name = "users")
public class User extends Aggregate {

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
        super(aggregateId, USER);
        setName(userDto.getName());
        setUsername(userDto.getUsername());
        this.role = Role.valueOf(userDto.getRole());
        setActive(false);
    }

    /*
        DELETED_STATE
     */
    public boolean deletedState() {
        if(getState() == DELETED) {
            return !isActive();
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if(!(deletedState())) {
            throw new TutorException(INVARIANT_BREAK, getAggregateId());
        }
    }

    public void remove() {
        if(isActive()) {
            throw new TutorException(USER_ACTIVE, this.getAggregateId());
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
    @Override
    public Set<String> getFieldsChangedByFunctionalities() {
        return Set.of("name", "username", "active");
    }

    @Override
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        return this;
    }


}
