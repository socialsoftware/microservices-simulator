package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;

@Entity
public abstract class Product extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "product")
    private ProductCategory productCategory;
    private String name;
    private String description;
    private Double listPriceInCents;

    public Product() {

    }

    public Product(Integer aggregateId, ProductCategory productCategory, ProductDto productDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(productDto.getName());
        setDescription(productDto.getDescription());
        setListPriceInCents(productDto.getListPriceInCents());
        setProductCategory(productCategory);
    }

    public Product(Product other) {
        super(other);
        setProductCategory(new ProductCategory(other.getProductCategory()));
        setName(other.getName());
        setDescription(other.getDescription());
        setListPriceInCents(other.getListPriceInCents());
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
        if (this.productCategory != null) {
            this.productCategory.setProduct(this);
        }
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


    // ============================================================================
    // INVARIANTS
    // ============================================================================

    public boolean invariantNameNotEmpty() {
        return this.name.length() > 0;
    }

    public boolean invariantPriceNonNegative() {
        return listPriceInCents >= 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!(invariantNameNotEmpty()
               && invariantPriceNonNegative())) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }

}