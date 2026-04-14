package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto;

@Entity
public class CartItem {
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private Integer quantity;
    private Double unitPriceInCents;
    @ManyToOne
    private Cart cart;

    public CartItem() {

    }

    public CartItem(CartItemDto cartItemDto) {
        setProductId(cartItemDto.getProductId());
        setQuantity(cartItemDto.getQuantity());
        setUnitPriceInCents(cartItemDto.getUnitPriceInCents());
    }


    public CartItem(CartItem other) {
        setQuantity(other.getQuantity());
        setUnitPriceInCents(other.getUnitPriceInCents());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }




    public CartItemDto buildDto() {
        CartItemDto dto = new CartItemDto();
        dto.setProductId(getProductId());
        dto.setQuantity(getQuantity());
        dto.setUnitPriceInCents(getUnitPriceInCents());
        return dto;
    }
}