package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class DiscountUpdatedEvent extends Event {
    @Column(name = "discount_updated_event_code")
    private String code;
    @Column(name = "discount_updated_event_description")
    private String description;
    @Column(name = "discount_updated_event_percentage_off")
    private Double percentageOff;
    @Column(name = "discount_updated_event_active")
    private Boolean active;
    @Column(name = "discount_updated_event_valid_from")
    private String validFrom;
    @Column(name = "discount_updated_event_valid_until")
    private String validUntil;

    public DiscountUpdatedEvent() {
        super();
    }

    public DiscountUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public DiscountUpdatedEvent(Integer aggregateId, String code, String description, Double percentageOff, Boolean active, String validFrom, String validUntil) {
        super(aggregateId);
        setCode(code);
        setDescription(description);
        setPercentageOff(percentageOff);
        setActive(active);
        setValidFrom(validFrom);
        setValidUntil(validUntil);
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