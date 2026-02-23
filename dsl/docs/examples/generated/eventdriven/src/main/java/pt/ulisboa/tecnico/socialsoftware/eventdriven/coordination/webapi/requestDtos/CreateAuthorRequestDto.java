package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateAuthorRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String bio;

    public CreateAuthorRequestDto() {}

    public CreateAuthorRequestDto(String name, String bio) {
        this.name = name;
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
