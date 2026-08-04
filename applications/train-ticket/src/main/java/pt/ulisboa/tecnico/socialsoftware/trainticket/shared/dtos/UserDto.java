package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.User;

public class UserDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String userName;
    private String password;
    private String gender;
    private String documentType;
    private String documentNum;
    private String email;
    private Double balance;

    public UserDto() {
    }

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.version = user.getVersion();
        this.state = user.getState();
        this.userName = user.getUserName();
        this.password = user.getPassword();
        this.gender = user.getGender() != null ? user.getGender().name() : null;
        this.documentType = user.getDocumentType() != null ? user.getDocumentType().name() : null;
        this.documentNum = user.getDocumentNum();
        this.email = user.getEmail();
        this.balance = user.getBalance();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
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