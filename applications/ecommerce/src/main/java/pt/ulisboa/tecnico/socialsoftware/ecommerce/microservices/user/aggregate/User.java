package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class User extends Aggregate {
    private String username;
    private String email;
    private String passwordHash;
    private String shippingAddress;

    public User() {

    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setUsername(userDto.getUsername());
        setEmail(userDto.getEmail());
        setPasswordHash(userDto.getPasswordHash());
        setShippingAddress(userDto.getShippingAddress());
    }


    public User(User other) {
        super(other);
        setUsername(other.getUsername());
        setEmail(other.getEmail());
        setPasswordHash(other.getPasswordHash());
        setShippingAddress(other.getShippingAddress());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.username != null && this.username.length() > 0;
    }

    private boolean invariantRule1() {
        return this.email != null && this.email.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Username cannot be empty");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Email cannot be empty");
        }
    }

    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUsername(getUsername());
        dto.setEmail(getEmail());
        dto.setPasswordHash(getPasswordHash());
        dto.setShippingAddress(getShippingAddress());
        return dto;
    }
}