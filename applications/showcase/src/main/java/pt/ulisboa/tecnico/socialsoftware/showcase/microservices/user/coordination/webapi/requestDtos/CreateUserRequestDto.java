package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.MembershipTier;

public class CreateUserRequestDto {
    @NotNull
    private String username;
    @NotNull
    private String email;
    @NotNull
    private Integer loyaltyPoints;
    @NotNull
    private MembershipTier tier;
    @NotNull
    private Boolean active;

    public CreateUserRequestDto() {}

    public CreateUserRequestDto(String username, String email, Integer loyaltyPoints, MembershipTier tier, Boolean active) {
        this.username = username;
        this.email = email;
        this.loyaltyPoints = loyaltyPoints;
        this.tier = tier;
        this.active = active;
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
}
