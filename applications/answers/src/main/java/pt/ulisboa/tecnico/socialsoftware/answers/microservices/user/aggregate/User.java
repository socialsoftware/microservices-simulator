package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

@Entity
public abstract class User extends Aggregate {
    private String name;
    private String username;
    @Enumerated(EnumType.STRING)
    private final UserRole role;
    private Boolean active;

    public User() {
        this.role = null;
    }

    public User(Integer aggregateId, UserDto userDto, UserRole role) {
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    @Override
    public void verifyInvariants() {
        // No invariants defined
    }
}