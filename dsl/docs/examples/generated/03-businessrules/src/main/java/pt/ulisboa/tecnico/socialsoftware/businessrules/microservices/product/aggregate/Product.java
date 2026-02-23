package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Product extends Aggregate {
    private String name;
    private String sku;
    private Double price;
    private Integer stockQuantity;
    private Boolean active;

    public Product() {

    }

    public Product(Integer aggregateId, ProductDto productDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(productDto.getName());
        setSku(productDto.getSku());
        setPrice(productDto.getPrice());
        setStockQuantity(productDto.getStockQuantity());
        setActive(productDto.getActive());
    }


    public Product(Product other) {
        super(other);
        setName(other.getName());
        setSku(other.getSku());
        setPrice(other.getPrice());
        setStockQuantity(other.getStockQuantity());
        setActive(other.getActive());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantNameNotBlank() {
        return this.name != null && this.name.length() > 0;
    }

    private boolean invariantPricePositive() {
        return price > 0.0;
    }

    private boolean invariantStockNonNegative() {
        return stockQuantity >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantNameNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product name cannot be blank");
        }
        if (!invariantPricePositive()) {
            throw new SimulatorException(INVARIANT_BREAK, "Product price must be positive");
        }
        if (!invariantStockNonNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Stock quantity cannot be negative");
        }
    }

    public ProductDto buildDto() {
        ProductDto dto = new ProductDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setSku(getSku());
        dto.setPrice(getPrice());
        dto.setStockQuantity(getStockQuantity());
        dto.setActive(getActive());
        return dto;
    }
}