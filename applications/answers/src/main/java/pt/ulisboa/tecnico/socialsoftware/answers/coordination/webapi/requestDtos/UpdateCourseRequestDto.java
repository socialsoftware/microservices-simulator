package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;
import java.time.LocalDateTime;

public class UpdateCourseRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String type;
    @NotNull
    private LocalDateTime creationDate;

    public UpdateCourseRequestDto() {}

    public UpdateCourseRequestDto(String name, String type, LocalDateTime creationDate) {
        this.name = name;
        this.type = type;
        this.creationDate = creationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
