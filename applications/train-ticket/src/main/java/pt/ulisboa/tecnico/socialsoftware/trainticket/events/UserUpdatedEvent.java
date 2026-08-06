package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class UserUpdatedEvent extends Event {
    private String userName;
    private String password;
    private String documentNum;
    private String email;
    private Double balance;

    public UserUpdatedEvent() {
        super();
    }

    public UserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public UserUpdatedEvent(Integer aggregateId, String userName, String password, String documentNum, String email, Double balance) {
        super(aggregateId);
        setUserName(userName);
        setPassword(password);
        setDocumentNum(documentNum);
        setEmail(email);
        setBalance(balance);
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
