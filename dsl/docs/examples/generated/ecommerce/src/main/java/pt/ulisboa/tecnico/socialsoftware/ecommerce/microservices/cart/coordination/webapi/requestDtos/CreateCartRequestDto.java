package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;

public class CreateCartRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private Double totalInCents;
    @NotNull
    private Integer itemCount;
    @NotNull
    private Boolean checkedOut;

    public CreateCartRequestDto() {}

    public CreateCartRequestDto(UserDto user, Double totalInCents, Integer itemCount, Boolean checkedOut) {
        this.user = user;
        this.totalInCents = totalInCents;
        this.itemCount = itemCount;
        this.checkedOut = checkedOut;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }
    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }
    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }
}
