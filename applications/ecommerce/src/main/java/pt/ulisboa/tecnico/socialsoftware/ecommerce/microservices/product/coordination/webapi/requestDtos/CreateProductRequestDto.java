package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateProductRequestDto {
    @NotNull
    private String sku;
    @NotNull
    private String name;
    @NotNull
    private String description;
    @NotNull
    private Double priceInCents;
    @NotNull
    private Integer stock;

    public CreateProductRequestDto() {}

    public CreateProductRequestDto(String sku, String name, String description, Double priceInCents, Integer stock) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.stock = stock;
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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public Double getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(Double priceInCents) {
        this.priceInCents = priceInCents;
    }
    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
