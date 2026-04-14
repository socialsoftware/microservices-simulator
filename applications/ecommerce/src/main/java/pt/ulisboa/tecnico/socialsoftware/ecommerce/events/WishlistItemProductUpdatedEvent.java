package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class WishlistItemProductUpdatedEvent extends Event {
    @Column(name = "wishlist_item_product_updated_event_product_aggregate_id")
    private Integer productAggregateId;
    @Column(name = "wishlist_item_product_updated_event_product_version")
    private Integer productVersion;
    @Column(name = "wishlist_item_product_updated_event_product_sku")
    private String productSku;
    @Column(name = "wishlist_item_product_updated_event_product_name")
    private String productName;
    @Column(name = "wishlist_item_product_updated_event_product_price_in_cents")
    private Double productPriceInCents;

    public WishlistItemProductUpdatedEvent() {
        super();
    }

    public WishlistItemProductUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public WishlistItemProductUpdatedEvent(Integer aggregateId, Integer productAggregateId, Integer productVersion, String productSku, String productName, Double productPriceInCents) {
        super(aggregateId);
        setProductAggregateId(productAggregateId);
        setProductVersion(productVersion);
        setProductSku(productSku);
        setProductName(productName);
        setProductPriceInCents(productPriceInCents);
    }

    public Integer getProductAggregateId() {
        return productAggregateId;
    }

    public void setProductAggregateId(Integer productAggregateId) {
        this.productAggregateId = productAggregateId;
    }

    public Integer getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(Integer productVersion) {
        this.productVersion = productVersion;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getProductPriceInCents() {
        return productPriceInCents;
    }

    public void setProductPriceInCents(Double productPriceInCents) {
        this.productPriceInCents = productPriceInCents;
    }

}