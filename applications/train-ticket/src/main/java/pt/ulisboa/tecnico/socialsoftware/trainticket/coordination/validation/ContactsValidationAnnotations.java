package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsUser;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;

public class ContactsValidationAnnotations {

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

    public static class DocumentNumberValidation {
        @NotNull
    @NotBlank
        private String documentNumber;
        
        public String getDocumentNumber() {
            return documentNumber;
        }
        
        public void setDocumentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
        }
    }

    public static class PhoneNumberValidation {
        @NotNull
    @NotBlank
        private String phoneNumber;
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    public static class UserValidation {
        @NotNull
        private ContactsUser user;
        
        public ContactsUser getUser() {
            return user;
        }
        
        public void setUser(ContactsUser user) {
            this.user = user;
        }
    }

}