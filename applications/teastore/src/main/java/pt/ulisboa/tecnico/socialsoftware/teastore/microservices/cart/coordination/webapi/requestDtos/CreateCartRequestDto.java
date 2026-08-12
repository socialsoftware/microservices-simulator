package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateCartRequestDto {
    @NotNull
    private Long userId;
    @NotNull
    private Boolean checkedOut;
    @NotNull
    private Double totalPrice;

    public CreateCartRequestDto() {}

    public CreateCartRequestDto(Long userId, Boolean checkedOut, Double totalPrice) {
        this.userId = userId;
        this.checkedOut = checkedOut;
        this.totalPrice = totalPrice;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }
    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
