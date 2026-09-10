package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType;

public class UserDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private String userName;
    private String password;
    private Gender gender;
    private DocumentType documentType;
    private String documentNumber;
    private String email;

    public UserDto() {
    }

    public UserDto(String userName, String password, Gender gender, DocumentType documentType,
                   String documentNumber, String email) {
        this.userName = userName;
        this.password = password;
        this.gender = gender;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.email = email;
    }

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.version = user.getVersion();
        this.state = user.getState();
        this.userName = user.getUserName();
        this.password = user.getPassword();
        this.gender = user.getGender();
        this.documentType = user.getDocumentType();
        this.documentNumber = user.getDocumentNumber();
        this.email = user.getEmail();
    }

    public boolean isActive() {
        return this.state == Aggregate.AggregateState.ACTIVE;
    }

    public Integer getAggregateId() {
        return this.aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Aggregate.AggregateState getState() {
        return this.state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Gender getGender() {
        return this.gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public DocumentType getDocumentType() {
        return this.documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return this.documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
