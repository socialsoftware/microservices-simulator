package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartItem;

public class CartItemDto implements Serializable {
    private Long productId;
    private Integer quantity;
    private Double unitPriceInCents;

    public CartItemDto() {
    }

    public CartItemDto(CartItem cartItem) {
        this.productId = cartItem.getProductId();
        this.quantity = cartItem.getQuantity();
        this.unitPriceInCents = cartItem.getUnitPriceInCents();
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPriceInCents() {
        return unitPriceInCents;
    }

    public void setUnitPriceInCents(Double unitPriceInCents) {
        this.unitPriceInCents = unitPriceInCents;
    }
}