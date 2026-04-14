package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateUserRequestDto {
    @NotNull
    private String username;
    @NotNull
    private String email;
    @NotNull
    private Integer loyaltyPoints;

    public CreateUserRequestDto() {}

    public CreateUserRequestDto(String username, String email, Integer loyaltyPoints) {
        this.username = username;
        this.email = email;
        this.loyaltyPoints = loyaltyPoints;
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
}
