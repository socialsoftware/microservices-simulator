package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Discount extends Aggregate {
    private String code;
    private String description;
    private Double percentageOff;
    private Boolean active;
    private String validFrom;
    private String validUntil;

    public Discount() {

    }

    public Discount(Integer aggregateId, DiscountDto discountDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCode(discountDto.getCode());
        setDescription(discountDto.getDescription());
        setPercentageOff(discountDto.getPercentageOff());
        setActive(discountDto.getActive());
        setValidFrom(discountDto.getValidFrom());
        setValidUntil(discountDto.getValidUntil());
    }


    public Discount(Discount other) {
        super(other);
        setCode(other.getCode());
        setDescription(other.getDescription());
        setPercentageOff(other.getPercentageOff());
        setActive(other.getActive());
        setValidFrom(other.getValidFrom());
        setValidUntil(other.getValidUntil());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.code != null && this.code.length() > 0;
    }

    private boolean invariantRule1() {
        return percentageOff >= 0.0 && percentageOff <= 100.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Discount code cannot be empty");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Discount percentage must be between 0 and 100");
        }
    }

    public DiscountDto buildDto() {
        DiscountDto dto = new DiscountDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setCode(getCode());
        dto.setDescription(getDescription());
        dto.setPercentageOff(getPercentageOff());
        dto.setActive(getActive());
        dto.setValidFrom(getValidFrom());
        dto.setValidUntil(getValidUntil());
        return dto;
    }
}