package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

public class UserValidationAnnotations {

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

    public static class UsernameValidation {
        @NotNull
    @NotBlank
        private String username;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }

    public static class RoleValidation {
        @NotNull
        private UserRole role;
        
        public UserRole getRole() {
            return role;
        }
        
        public void setRole(UserRole role) {
            this.role = role;
        }
    }

    public static class ActiveValidation {
        @NotNull
        private Boolean active;
        
        public Boolean getActive() {
            return active;
        }
        
        public void setActive(Boolean active) {
            this.active = active;
        }
    }

}