package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

public class CreateUserRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String username;
    @NotNull
    private UserRole role;
    @NotNull
    private Boolean active;

    public CreateUserRequestDto() {}

    public CreateUserRequestDto(String name, String username, UserRole role, Boolean active) {
        this.name = name;
        this.username = username;
        this.role = role;
        this.active = active;
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

    public void setRole(UserRole role) {
        this.role = role;
    }
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
