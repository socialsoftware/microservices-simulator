package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.enums.MembershipType;

public class CreateMemberRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String email;
    @NotNull
    private MembershipType membership;

    public CreateMemberRequestDto() {}

    public CreateMemberRequestDto(String name, String email, MembershipType membership) {
        this.name = name;
        this.email = email;
        this.membership = membership;
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
    public MembershipType getMembership() {
        return membership;
    }

    public void setMembership(MembershipType membership) {
        this.membership = membership;
    }
}
