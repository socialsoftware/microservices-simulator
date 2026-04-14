package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.MembershipTier;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class User extends Aggregate {
    private String username;
    private String email;
    private Integer loyaltyPoints;
    @Enumerated(EnumType.STRING)
    private MembershipTier tier;
    private Boolean active;

    public User() {
        this.tier = MembershipTier.BRONZE;
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setUsername(userDto.getUsername());
        setEmail(userDto.getEmail());
        setLoyaltyPoints(userDto.getLoyaltyPoints());
        setTier(userDto.getTier() != null ? MembershipTier.valueOf(userDto.getTier()) : MembershipTier.BRONZE);
        setActive(userDto.getActive());
    }


    public User(User other) {
        super(other);
        setUsername(other.getUsername());
        setEmail(other.getEmail());
        setLoyaltyPoints(other.getLoyaltyPoints());
        setTier(other.getTier());
        setActive(other.getActive());
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

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public void setTier(MembershipTier tier) {
        this.tier = tier;
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
        return this.username != null && this.username.length() > 0;
    }

    private boolean invariantRule1() {
        return this.email != null && this.email.length() > 0;
    }

    private boolean invariantRule2() {
        return loyaltyPoints >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Username cannot be empty");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Email cannot be empty");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "Loyalty points cannot be negative");
        }
    }

    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUsername(getUsername());
        dto.setEmail(getEmail());
        dto.setLoyaltyPoints(getLoyaltyPoints());
        dto.setTier(getTier() != null ? getTier().name() : null);
        dto.setActive(getActive());
        return dto;
    }
}