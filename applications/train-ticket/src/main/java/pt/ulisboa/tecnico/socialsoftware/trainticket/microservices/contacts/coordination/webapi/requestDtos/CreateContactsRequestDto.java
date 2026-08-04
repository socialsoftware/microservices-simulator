package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;

public class CreateContactsRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private String name;
    @NotNull
    private DocumentType documentType;
    @NotNull
    private String documentNumber;
    @NotNull
    private String phoneNumber;

    public CreateContactsRequestDto() {}

    public CreateContactsRequestDto(UserDto user, String name, DocumentType documentType, String documentNumber, String phoneNumber) {
        this.user = user;
        this.name = name;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.phoneNumber = phoneNumber;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }
    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
