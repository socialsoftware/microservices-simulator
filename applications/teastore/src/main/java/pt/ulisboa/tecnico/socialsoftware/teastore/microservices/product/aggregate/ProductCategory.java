package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

@Entity
public class ProductCategory {
    @Id
    @GeneratedValue
    private Long id;
    private Integer categoryAggregateId;
    private String categoryName;
    private String categoryDescription;
    @OneToOne
    private Product product;

    public ProductCategory() {

    }

    public ProductCategory(CategoryDto categoryDto) {
        setCategoryAggregateId(categoryDto.getAggregateId());
        setCategoryName(categoryDto.getName());
        setCategoryDescription(categoryDto.getDescription());
    }

    public ProductCategory(ProductCategory other) {
        setCategoryAggregateId(other.getCategoryAggregateId());
        setCategoryName(other.getCategoryName());
        setCategoryDescription(other.getCategoryDescription());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCategoryAggregateId() {
        return categoryAggregateId;
    }

    public void setCategoryAggregateId(Integer categoryAggregateId) {
        this.categoryAggregateId = categoryAggregateId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }


    public CategoryDto buildDto() {
        CategoryDto dto = new CategoryDto();
        dto.setAggregateId(getCategoryAggregateId());
        dto.setName(getCategoryName());
        dto.setDescription(getCategoryDescription());
        return dto;
    }
}