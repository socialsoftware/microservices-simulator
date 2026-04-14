package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemProduct;

public class WishlistItemProductDto implements Serializable {
    private String sku;
    private String name;
    private Double priceInCents;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public WishlistItemProductDto() {
    }

    public WishlistItemProductDto(WishlistItemProduct wishlistItemProduct) {
        this.sku = wishlistItemProduct.getProductSku();
        this.name = wishlistItemProduct.getProductName();
        this.priceInCents = wishlistItemProduct.getProductPriceInCents();
        this.aggregateId = wishlistItemProduct.getProductAggregateId();
        this.version = wishlistItemProduct.getProductVersion();
        this.state = wishlistItemProduct.getProductState() != null ? wishlistItemProduct.getProductState().name() : null;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(Double priceInCents) {
        this.priceInCents = priceInCents;
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