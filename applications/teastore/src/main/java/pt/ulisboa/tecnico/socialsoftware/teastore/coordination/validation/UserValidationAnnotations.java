package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class UserValidationAnnotations {

    public static class UserNameValidation {
        @NotNull
    @NotBlank
        private String userName;
        
        public String getUserName() {
            return userName;
        }
        
        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class PasswordValidation {
        @NotNull
    @NotBlank
        private String password;
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RealNameValidation {
        @NotNull
    @NotBlank
        private String realName;
        
        public String getRealName() {
            return realName;
        }
        
        public void setRealName(String realName) {
            this.realName = realName;
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

}