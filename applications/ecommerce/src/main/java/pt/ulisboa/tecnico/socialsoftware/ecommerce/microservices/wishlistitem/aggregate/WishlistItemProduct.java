package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemProductDto;

@Entity
public class WishlistItemProduct {
    @Id
    @GeneratedValue
    private Long id;
    private String productSku;
    private String productName;
    private Double productPriceInCents;
    private Integer productAggregateId;
    private Integer productVersion;
    private AggregateState productState;
    @OneToOne
    private WishlistItem wishlistItem;

    public WishlistItemProduct() {

    }

    public WishlistItemProduct(ProductDto productDto) {
        setProductAggregateId(productDto.getAggregateId());
        setProductVersion(productDto.getVersion());
        setProductState(productDto.getState());
    }

    public WishlistItemProduct(WishlistItemProductDto wishlistItemProductDto) {
        setProductSku(wishlistItemProductDto.getSku());
        setProductName(wishlistItemProductDto.getName());
        setProductPriceInCents(wishlistItemProductDto.getPriceInCents());
        setProductAggregateId(wishlistItemProductDto.getAggregateId());
        setProductVersion(wishlistItemProductDto.getVersion());
        setProductState(wishlistItemProductDto.getState() != null ? AggregateState.valueOf(wishlistItemProductDto.getState()) : null);
    }

    public WishlistItemProduct(WishlistItemProduct other) {
        setProductSku(other.getProductSku());
        setProductName(other.getProductName());
        setProductPriceInCents(other.getProductPriceInCents());
        setProductAggregateId(other.getProductAggregateId());
        setProductVersion(other.getProductVersion());
        setProductState(other.getProductState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AggregateState getProductState() {
        return productState;
    }

    public void setProductState(AggregateState productState) {
        this.productState = productState;
    }

    public WishlistItem getWishlistItem() {
        return wishlistitem;
    }

    public void setWishlistItem(WishlistItem wishlistitem) {
        this.wishlistitem = wishlistitem;
    }




    public WishlistItemProductDto buildDto() {
        WishlistItemProductDto dto = new WishlistItemProductDto();
        dto.setSku(getProductSku());
        dto.setName(getProductName());
        dto.setPriceInCents(getProductPriceInCents());
        dto.setAggregateId(getProductAggregateId());
        dto.setVersion(getProductVersion());
        dto.setState(getProductState() != null ? getProductState().name() : null);
        return dto;
    }
}