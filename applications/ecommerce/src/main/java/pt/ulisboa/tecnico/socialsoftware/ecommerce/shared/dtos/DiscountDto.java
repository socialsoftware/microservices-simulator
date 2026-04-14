package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.Discount;

public class DiscountDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String code;
    private String description;
    private Double percentageOff;
    private Boolean active;
    private String validFrom;
    private String validUntil;

    public DiscountDto() {
    }

    public DiscountDto(Discount discount) {
        this.aggregateId = discount.getAggregateId();
        this.version = discount.getVersion();
        this.state = discount.getState();
        this.code = discount.getCode();
        this.description = discount.getDescription();
        this.percentageOff = discount.getPercentageOff();
        this.active = discount.getActive();
        this.validFrom = discount.getValidFrom();
        this.validUntil = discount.getValidUntil();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPercentageOff() {
        return percentageOff;
    }

    public void setPercentageOff(Double percentageOff) {
        this.percentageOff = percentageOff;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
}