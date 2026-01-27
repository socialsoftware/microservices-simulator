package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

@Entity
public abstract class User extends Aggregate {
    private String name;
    private String username;
    @Enumerated(EnumType.STRING)
    private final UserRole role;
    private boolean active;

    public User() {
        this.role = null;
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(userDto.getName());
        setUsername(userDto.getUsername());
        this.role = UserRole.valueOf(userDto.getRole());
        setActive(userDto.getActive());
    }

    public User(User other) {
        super(other);
        setName(other.getName());
        setUsername(other.getUsername());
        this.role = other.getRole();
        setActive(other.getActive());
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

    public UserRole getRole() {
        return role;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setUsername(getUsername());
        dto.setRole(getRole() != null ? getRole().name() : null);
        dto.setActive(getActive());
        return dto;
    }
}