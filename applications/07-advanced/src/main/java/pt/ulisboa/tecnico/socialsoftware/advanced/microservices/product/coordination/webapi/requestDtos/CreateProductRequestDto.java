package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateProductRequestDto {
    @NotNull
    private String name;
    @NotNull
    private Double price;
    @NotNull
    private Boolean available;

    public CreateProductRequestDto() {}

    public CreateProductRequestDto(String name, Double price, Boolean available) {
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
