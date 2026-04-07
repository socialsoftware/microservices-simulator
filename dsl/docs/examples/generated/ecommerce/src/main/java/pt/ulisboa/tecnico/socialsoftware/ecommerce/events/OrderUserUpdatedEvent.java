package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderUserUpdatedEvent extends Event {
    @Column(name = "order_user_updated_event_user_aggregate_id")
    private Integer userAggregateId;
    @Column(name = "order_user_updated_event_user_version")
    private Integer userVersion;
    @Column(name = "order_user_updated_event_user_name")
    private String userName;
    @Column(name = "order_user_updated_event_user_email")
    private String userEmail;
    @Column(name = "order_user_updated_event_shipping_address")
    private String shippingAddress;

    public OrderUserUpdatedEvent() {
        super();
    }

    public OrderUserUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userEmail, String shippingAddress) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setUserVersion(userVersion);
        setUserName(userName);
        setUserEmail(userEmail);
        setShippingAddress(shippingAddress);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Integer userVersion) {
        this.userVersion = userVersion;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

}