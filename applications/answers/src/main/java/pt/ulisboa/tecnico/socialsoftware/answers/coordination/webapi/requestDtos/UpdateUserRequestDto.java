package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

public class UpdateUserRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String username;
    @NotNull
    private String role;
    @NotNull
    private Boolean active;

    public UpdateUserRequestDto() {}

    public UpdateUserRequestDto(String name, String username, String role, Boolean active) {
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
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
