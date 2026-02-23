package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateCustomerRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String email;
    @NotNull
    private Boolean active;

    public CreateCustomerRequestDto() {}

    public CreateCustomerRequestDto(String name, String email, Boolean active) {
        this.name = name;
        this.email = email;
        this.active = active;
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
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
