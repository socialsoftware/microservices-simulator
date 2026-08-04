package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.Gender;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;

public class CreateUserRequestDto {
    @NotNull
    private String userName;
    @NotNull
    private String password;
    @NotNull
    private Gender gender;
    @NotNull
    private DocumentType documentType;
    @NotNull
    private String documentNum;
    @NotNull
    private String email;
    @NotNull
    private Double balance;

    public CreateUserRequestDto() {}

    public CreateUserRequestDto(String userName, String password, Gender gender, DocumentType documentType, String documentNum, String email, Double balance) {
        this.userName = userName;
        this.password = password;
        this.gender = gender;
        this.documentType = documentType;
        this.documentNum = documentNum;
        this.email = email;
        this.balance = balance;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }
    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }
    public String getDocumentNum() {
        return documentNum;
    }

    public void setDocumentNum(String documentNum) {
        this.documentNum = documentNum;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
