package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;
import java.time.LocalDateTime;

public class CreateCourseRequestDto {
    @NotNull
    private String name;
    @NotNull
    private CourseType type;
    @NotNull
    private LocalDateTime creationDate;

    public CreateCourseRequestDto() {}

    public CreateCourseRequestDto(String name, CourseType type, LocalDateTime creationDate) {
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
    public CourseType getType() {
        return type;
    }

    public void setType(CourseType type) {
        this.type = type;
    }
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
