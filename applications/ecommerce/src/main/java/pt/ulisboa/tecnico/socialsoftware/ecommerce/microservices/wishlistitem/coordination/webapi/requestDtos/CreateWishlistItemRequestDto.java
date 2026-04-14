package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto;

public class CreateWishlistItemRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private ProductDto product;
    @NotNull
    private String addedAt;

    public CreateWishlistItemRequestDto() {}

    public CreateWishlistItemRequestDto(UserDto user, ProductDto product, String addedAt) {
        this.user = user;
        this.product = product;
        this.addedAt = addedAt;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public ProductDto getProduct() {
        return product;
    }

    public void setProduct(ProductDto product) {
        this.product = product;
    }
    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }
}
