package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;

public class WishlistItemDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private WishlistItemUserDto user;
    private WishlistItemProductDto product;
    private String addedAt;

    public WishlistItemDto() {
    }

    public WishlistItemDto(WishlistItem wishlistItem) {
        this.aggregateId = wishlistItem.getAggregateId();
        this.version = wishlistItem.getVersion();
        this.state = wishlistItem.getState();
        this.user = wishlistItem.getUser() != null ? new WishlistItemUserDto(wishlistItem.getUser()) : null;
        this.product = wishlistItem.getProduct() != null ? new WishlistItemProductDto(wishlistItem.getProduct()) : null;
        this.addedAt = wishlistItem.getAddedAt();
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

    public WishlistItemUserDto getUser() {
        return user;
    }

    public void setUser(WishlistItemUserDto user) {
        this.user = user;
    }

    public WishlistItemProductDto getProduct() {
        return product;
    }

    public void setProduct(WishlistItemProductDto product) {
        this.product = product;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }
}