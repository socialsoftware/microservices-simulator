package pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateProductRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String sku;
    @NotNull
    private Double price;
    @NotNull
    private Integer stockQuantity;
    @NotNull
    private Boolean active;

    public CreateProductRequestDto() {}

    public CreateProductRequestDto(String name, String sku, Double price, Integer stockQuantity, Boolean active) {
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
