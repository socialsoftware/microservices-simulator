package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserUpdatedEvent extends Event {
    private String username;
    private String email;
    private String shippingAddress;

    public UserUpdatedEvent() {
        super();
    }

    public UserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public UserUpdatedEvent(Integer aggregateId, String username, String email, String shippingAddress) {
        super(aggregateId);
        setUsername(username);
        setEmail(email);
        setShippingAddress(shippingAddress);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

}