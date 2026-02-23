package pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateTaskRequestDto {
    @NotNull
    private String title;
    @NotNull
    private String description;
    @NotNull
    private Boolean done;

    public CreateTaskRequestDto() {}

    public CreateTaskRequestDto(String title, String description, Boolean done) {
        this.title = title;
        this.description = description;
        this.done = done;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
}
