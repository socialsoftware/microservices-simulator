package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartUser;

public class CartUserDto implements Serializable {
    private String username;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public CartUserDto() {
    }

    public CartUserDto(CartUser cartUser) {
        this.username = cartUser.getUserName();
        this.email = cartUser.getUserEmail();
        this.aggregateId = cartUser.getUserAggregateId();
        this.version = cartUser.getUserVersion();
        this.state = cartUser.getUserState() != null ? cartUser.getUserState().name() : null;
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