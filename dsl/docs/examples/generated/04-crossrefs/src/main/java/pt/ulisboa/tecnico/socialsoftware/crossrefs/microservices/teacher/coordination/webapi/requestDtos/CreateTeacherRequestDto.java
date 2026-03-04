package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateTeacherRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String email;
    @NotNull
    private String department;

    public CreateTeacherRequestDto() {}

    public CreateTeacherRequestDto(String name, String email, String department) {
        this.name = name;
        this.email = email;
        this.department = department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
