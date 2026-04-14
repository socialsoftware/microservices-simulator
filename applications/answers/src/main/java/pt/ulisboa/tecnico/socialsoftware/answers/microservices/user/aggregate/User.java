package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.name != null && this.name.length() > 0;
    }

    private boolean invariantRule1() {
        return this.username != null && this.username.length() > 0;
    }

    private boolean invariantRule2() {
        return this.role != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "User name cannot be blank");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Username cannot be blank");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "User role is required");
        }
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