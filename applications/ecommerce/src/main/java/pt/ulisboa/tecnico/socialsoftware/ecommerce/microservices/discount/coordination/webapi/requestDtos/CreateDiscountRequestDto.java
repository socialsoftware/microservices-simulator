package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateDiscountRequestDto {
    @NotNull
    private String code;
    @NotNull
    private String description;
    @NotNull
    private Double percentageOff;
    @NotNull
    private Boolean active;
    @NotNull
    private String validFrom;
    @NotNull
    private String validUntil;

    public CreateDiscountRequestDto() {}

    public CreateDiscountRequestDto(String code, String description, Double percentageOff, Boolean active, String validFrom, String validUntil) {
        this.code = code;
        this.description = description;
        this.percentageOff = percentageOff;
        this.active = active;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
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
