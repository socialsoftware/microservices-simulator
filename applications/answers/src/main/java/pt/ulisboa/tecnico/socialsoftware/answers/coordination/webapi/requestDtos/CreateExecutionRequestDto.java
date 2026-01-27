package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import java.util.Set;
import java.time.LocalDateTime;

public class CreateExecutionRequestDto {
    @NotNull
    private CourseDto course;
    @NotNull
    private Set<UserDto> users;
    @NotNull
    private String acronym;
    @NotNull
    private String academicTerm;
    @NotNull
    private LocalDateTime endDate;

    public CreateExecutionRequestDto() {}

    public CreateExecutionRequestDto(CourseDto course, Set<UserDto> users, String acronym, String academicTerm, LocalDateTime endDate) {
        this.course = course;
        this.users = users;
        this.acronym = acronym;
        this.academicTerm = academicTerm;
        this.endDate = endDate;
    }

    public CourseDto getCourse() {
        return course;
    }

    public void setCourse(CourseDto course) {
        this.course = course;
    }
    public Set<UserDto> getUsers() {
        return users;
    }

    public void setUsers(Set<UserDto> users) {
        this.users = users;
    }
    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    public String getAcademicTerm() {
        return academicTerm;
    }

    public void setAcademicTerm(String academicTerm) {
        this.academicTerm = academicTerm;
    }
    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
