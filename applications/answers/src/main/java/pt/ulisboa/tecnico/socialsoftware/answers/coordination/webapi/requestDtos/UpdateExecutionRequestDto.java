package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import java.util.Set;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;

public class UpdateExecutionRequestDto {
    @NotNull
    private String acronym;
    @NotNull
    private String academicTerm;
    @NotNull
    private LocalDateTime endDate;
    @NotNull
    private ExecutionCourseDto course;
    @NotNull
    private Set<ExecutionUserDto> users;

    public UpdateExecutionRequestDto() {}

    public UpdateExecutionRequestDto(String acronym, String academicTerm, LocalDateTime endDate, ExecutionCourseDto course, Set<ExecutionUserDto> users) {
        this.acronym = acronym;
        this.academicTerm = academicTerm;
        this.endDate = endDate;
        this.course = course;
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
    public ExecutionCourseDto getCourse() {
        return course;
    }

    public void setCourse(ExecutionCourseDto course) {
        this.course = course;
    }
    public Set<ExecutionUserDto> getUsers() {
        return users;
    }

    public void setUsers(Set<ExecutionUserDto> users) {
        this.users = users;
    }
}
