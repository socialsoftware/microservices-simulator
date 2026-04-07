package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderUser;

public class OrderUserDto implements Serializable {
    private String username;
    private String email;
    private String shippingAddress;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public OrderUserDto() {
    }

    public OrderUserDto(OrderUser orderUser) {
        this.username = orderUser.getUserName();
        this.email = orderUser.getUserEmail();
        this.shippingAddress = orderUser.getShippingAddress();
        this.aggregateId = orderUser.getUserAggregateId();
        this.version = orderUser.getUserVersion();
        this.state = orderUser.getUserState() != null ? orderUser.getUserState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}