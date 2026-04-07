package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class UserValidationAnnotations {

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

    public static class PasswordHashValidation {
        @NotNull
    @NotBlank
        private String passwordHash;
        
        public String getPasswordHash() {
            return passwordHash;
        }
        
        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }

    public static class ShippingAddressValidation {
        @NotNull
    @NotBlank
        private String shippingAddress;
        
        public String getShippingAddress() {
            return shippingAddress;
        }
        
        public void setShippingAddress(String shippingAddress) {
            this.shippingAddress = shippingAddress;
        }
    }

}