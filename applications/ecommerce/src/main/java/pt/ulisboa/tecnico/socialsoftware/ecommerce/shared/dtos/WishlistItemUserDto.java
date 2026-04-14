package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemUser;

public class WishlistItemUserDto implements Serializable {
    private String username;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public WishlistItemUserDto() {
    }

    public WishlistItemUserDto(WishlistItemUser wishlistItemUser) {
        this.username = wishlistItemUser.getUserName();
        this.email = wishlistItemUser.getUserEmail();
        this.aggregateId = wishlistItemUser.getUserAggregateId();
        this.version = wishlistItemUser.getUserVersion();
        this.state = wishlistItemUser.getUserState() != null ? wishlistItemUser.getUserState().name() : null;
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