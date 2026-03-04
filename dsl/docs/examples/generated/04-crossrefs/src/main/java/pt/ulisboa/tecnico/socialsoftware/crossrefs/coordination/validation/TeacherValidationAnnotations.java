package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class TeacherValidationAnnotations {

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

    public static class DepartmentValidation {
        @NotNull
    @NotBlank
        private String department;
        
        public String getDepartment() {
            return department;
        }
        
        public void setDepartment(String department) {
            this.department = department;
        }
    }

}