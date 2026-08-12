package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.subscribe.ProductSubscribesCategoryDeletedProductCategoryExists;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.subscribe.ProductSubscribesCategoryUpdated;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductCategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Product extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "product")
    private ProductCategory productCategory;
    private String name;
    private String description;
    private Double listPriceInCents;

    public Product() {

    }

    public Product(Integer aggregateId, ProductDto productDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(productDto.getName());
        setDescription(productDto.getDescription());
        setListPriceInCents(productDto.getListPriceInCents());
        setProductCategory(productDto.getProductCategory() != null ? new ProductCategory(productDto.getProductCategory()) : null);
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantProductCategoryExists(eventSubscriptions);
            eventSubscriptions.add(new ProductSubscribesCategoryUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantProductCategoryExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new ProductSubscribesCategoryDeletedProductCategoryExists(this.getProductCategory()));
    }


    private boolean invariantNameNotEmpty() {
        return this.name != null && this.name.length() > 0;
    }

    private boolean invariantPriceNonNegative() {
        return listPriceInCents >= 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantNameNotEmpty()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product name cannot be empty");
        }
        if (!invariantPriceNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product price cannot be negative");
        }
    }

    public ProductDto buildDto() {
        ProductDto dto = new ProductDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setProductCategory(getProductCategory() != null ? new ProductCategoryDto(getProductCategory()) : null);
        dto.setName(getName());
        dto.setDescription(getDescription());
        dto.setListPriceInCents(getListPriceInCents());
        return dto;
    }
}