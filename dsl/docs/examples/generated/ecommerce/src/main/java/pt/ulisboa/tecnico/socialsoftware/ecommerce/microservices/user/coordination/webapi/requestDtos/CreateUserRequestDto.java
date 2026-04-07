package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateUserRequestDto {
    @NotNull
    private String username;
    @NotNull
    private String email;
    @NotNull
    private String passwordHash;
    @NotNull
    private String shippingAddress;

    public CreateUserRequestDto() {}

    public CreateUserRequestDto(String username, String email, String passwordHash, String shippingAddress) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.shippingAddress = shippingAddress;
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
}
