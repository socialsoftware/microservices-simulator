package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.enums.MembershipType;

public class MemberValidationAnnotations {

    public static class NameValidation {
        @NotNull
    @NotBlank
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class EmailValidation {
        @NotNull
    @NotBlank
    @Email
        private String email;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class MembershipValidation {
        @NotNull
        private MembershipType membership;
        
        public MembershipType getMembership() {
            return membership;
        }
        
        public void setMembership(MembershipType membership) {
            this.membership = membership;
        }
    }

}