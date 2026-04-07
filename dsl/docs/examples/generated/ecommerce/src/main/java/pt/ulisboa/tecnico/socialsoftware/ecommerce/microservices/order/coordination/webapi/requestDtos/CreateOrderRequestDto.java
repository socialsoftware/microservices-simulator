package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.OrderStatus;

public class CreateOrderRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private Double totalInCents;
    @NotNull
    private Integer itemCount;
    @NotNull
    private OrderStatus status;
    @NotNull
    private String placedAt;

    public CreateOrderRequestDto() {}

    public CreateOrderRequestDto(UserDto user, Double totalInCents, Integer itemCount, OrderStatus status, String placedAt) {
        this.user = user;
        this.totalInCents = totalInCents;
        this.itemCount = itemCount;
        this.status = status;
        this.placedAt = placedAt;
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
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public String getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(String placedAt) {
        this.placedAt = placedAt;
    }
}
