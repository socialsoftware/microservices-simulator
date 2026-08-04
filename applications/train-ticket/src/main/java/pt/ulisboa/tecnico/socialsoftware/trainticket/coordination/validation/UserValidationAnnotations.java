package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.Gender;

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

    public static class GenderValidation {
        @NotNull
        private Gender gender;
        
        public Gender getGender() {
            return gender;
        }
        
        public void setGender(Gender gender) {
            this.gender = gender;
        }
    }

    public static class DocumentTypeValidation {
        @NotNull
        private DocumentType documentType;
        
        public DocumentType getDocumentType() {
            return documentType;
        }
        
        public void setDocumentType(DocumentType documentType) {
            this.documentType = documentType;
        }
    }

    public static class DocumentNumValidation {
        @NotNull
    @NotBlank
        private String documentNum;
        
        public String getDocumentNum() {
            return documentNum;
        }
        
        public void setDocumentNum(String documentNum) {
            this.documentNum = documentNum;
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

    public static class BalanceValidation {
        @NotNull
        private Double balance;
        
        public Double getBalance() {
            return balance;
        }
        
        public void setBalance(Double balance) {
            this.balance = balance;
        }
    }

}