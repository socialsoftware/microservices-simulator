package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserUpdatedEvent extends Event {
    private String userName;
    private String password;
    private String realName;
    private String email;

    public UserUpdatedEvent() {
    }

    public UserUpdatedEvent(Integer aggregateId, String userName, String password, String realName, String email) {
        super(aggregateId);
        setUserName(userName);
        setPassword(password);
        setRealName(realName);
        setEmail(email);
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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}