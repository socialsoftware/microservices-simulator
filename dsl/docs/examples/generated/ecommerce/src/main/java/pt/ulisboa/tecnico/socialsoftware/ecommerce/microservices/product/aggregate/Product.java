package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Product extends Aggregate {
    private String sku;
    private String name;
    private String description;
    private Double priceInCents;
    private Integer stock;

    public Product() {

    }

    public Product(Integer aggregateId, ProductDto productDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setSku(productDto.getSku());
        setName(productDto.getName());
        setDescription(productDto.getDescription());
        setPriceInCents(productDto.getPriceInCents());
        setStock(productDto.getStock());
    }


    public Product(Product other) {
        super(other);
        setSku(other.getSku());
        setName(other.getName());
        setDescription(other.getDescription());
        setPriceInCents(other.getPriceInCents());
        setStock(other.getStock());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantSkuNotEmpty() {
        return this.sku != null && this.sku.length() > 0;
    }

    private boolean invariantPriceNonNegative() {
        return priceInCents >= 0.0;
    }

    private boolean invariantStockNonNegative() {
        return stock >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantSkuNotEmpty()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product SKU cannot be empty");
        }
        if (!invariantPriceNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product price cannot be negative");
        }
        if (!invariantStockNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product stock cannot be negative");
        }
    }

    public ProductDto buildDto() {
        ProductDto dto = new ProductDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setSku(getSku());
        dto.setName(getName());
        dto.setDescription(getDescription());
        dto.setPriceInCents(getPriceInCents());
        dto.setStock(getStock());
        return dto;
    }
}