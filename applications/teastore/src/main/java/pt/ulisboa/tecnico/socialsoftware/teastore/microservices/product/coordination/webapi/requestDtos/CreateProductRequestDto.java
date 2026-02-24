package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

public class CreateProductRequestDto {
    @NotNull
    private CategoryDto productCategory;
    @NotNull
    private String name;
    @NotNull
    private String description;
    @NotNull
    private Double listPriceInCents;

    public CreateProductRequestDto() {}

    public CreateProductRequestDto(CategoryDto productCategory, String name, String description, Double listPriceInCents) {
        this.productCategory = productCategory;
        this.name = name;
        this.description = description;
        this.listPriceInCents = listPriceInCents;
    }

    public CategoryDto getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(CategoryDto productCategory) {
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
