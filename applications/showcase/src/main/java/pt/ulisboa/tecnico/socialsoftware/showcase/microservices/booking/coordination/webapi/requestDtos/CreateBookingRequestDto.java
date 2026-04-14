package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;

public class CreateBookingRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private RoomDto room;
    @NotNull
    private String checkInDate;
    @NotNull
    private String checkOutDate;
    @NotNull
    private Integer numberOfNights;
    @NotNull
    private Double totalPrice;

    public CreateBookingRequestDto() {}

    public CreateBookingRequestDto(UserDto user, RoomDto room, String checkInDate, String checkOutDate, Integer numberOfNights, Double totalPrice) {
        this.user = user;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfNights = numberOfNights;
        this.totalPrice = totalPrice;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public RoomDto getRoom() {
        return room;
    }

    public void setRoom(RoomDto room) {
        this.room = room;
    }
    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }
    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    public Integer getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(Integer numberOfNights) {
        this.numberOfNights = numberOfNights;
    }
    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
