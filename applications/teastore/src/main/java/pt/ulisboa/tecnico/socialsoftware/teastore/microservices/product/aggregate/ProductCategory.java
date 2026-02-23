package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductCategoryDto;

@Entity
public class ProductCategory {
    @Id
    @GeneratedValue
    private Long id;
    private String categoryName;
    private String categoryDescription;
    private Integer categoryAggregateId;
    private Integer categoryVersion;
    private AggregateState categoryState;
    @OneToOne
    private Product product;

    public ProductCategory() {

    }

    public ProductCategory(CategoryDto categoryDto) {
        setCategoryAggregateId(categoryDto.getAggregateId());
        setCategoryVersion(categoryDto.getVersion());
        setCategoryState(categoryDto.getState());
    }

    public ProductCategory(ProductCategoryDto productCategoryDto) {
        setCategoryName(productCategoryDto.getName());
        setCategoryDescription(productCategoryDto.getDescription());
        setCategoryAggregateId(productCategoryDto.getAggregateId());
        setCategoryVersion(productCategoryDto.getVersion());
        setCategoryState(productCategoryDto.getState());
    }

    public ProductCategory(ProductCategory other) {
        setCategoryName(other.getCategoryName());
        setCategoryDescription(other.getCategoryDescription());
        setCategoryAggregateId(other.getCategoryAggregateId());
        setCategoryVersion(other.getCategoryVersion());
        setCategoryState(other.getCategoryState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getCategoryAggregateId() {
        return categoryAggregateId;
    }

    public void setCategoryAggregateId(Integer categoryAggregateId) {
        this.categoryAggregateId = categoryAggregateId;
    }

    public Integer getCategoryVersion() {
        return categoryVersion;
    }

    public void setCategoryVersion(Integer categoryVersion) {
        this.categoryVersion = categoryVersion;
    }

    public AggregateState getCategoryState() {
        return categoryState;
    }

    public void setCategoryState(AggregateState categoryState) {
        this.categoryState = categoryState;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }




    public ProductCategoryDto buildDto() {
        ProductCategoryDto dto = new ProductCategoryDto();
        dto.setName(getCategoryName());
        dto.setDescription(getCategoryDescription());
        dto.setAggregateId(getCategoryAggregateId());
        dto.setVersion(getCategoryVersion());
        dto.setState(getCategoryState());
        return dto;
    }
}