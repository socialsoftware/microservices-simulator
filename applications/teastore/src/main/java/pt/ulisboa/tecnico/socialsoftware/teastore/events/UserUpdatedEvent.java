package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class UserUpdatedEvent extends Event {
    @Column(name = "user_updated_event_user_name")
    private String userName;
    @Column(name = "user_updated_event_password")
    private String password;
    @Column(name = "user_updated_event_real_name")
    private String realName;
    @Column(name = "user_updated_event_email")
    private String email;

    public UserUpdatedEvent() {
        super();
    }

    public UserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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