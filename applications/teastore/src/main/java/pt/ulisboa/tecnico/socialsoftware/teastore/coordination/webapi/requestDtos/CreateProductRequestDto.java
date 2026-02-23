package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.enums.ProductCategory;

public class CreateProductRequestDto {
    @NotNull
    private ProductCategory productCategory;
    @NotNull
    private String name;
    @NotNull
    private String description;
    @NotNull
    private Double listPriceInCents;

    public CreateProductRequestDto() {}

    public CreateProductRequestDto(ProductCategory productCategory, String name, String description, Double listPriceInCents) {
        this.productCategory = productCategory;
        this.name = name;
        this.description = description;
        this.listPriceInCents = listPriceInCents;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
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
    public Double getListPriceInCents() {
        return listPriceInCents;
    }

    public void setListPriceInCents(Double listPriceInCents) {
        this.listPriceInCents = listPriceInCents;
    }
}
