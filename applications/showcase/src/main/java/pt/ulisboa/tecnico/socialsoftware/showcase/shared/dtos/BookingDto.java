package pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;

public class BookingDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private BookingUserDto user;
    private BookingRoomDto room;
    private String checkInDate;
    private String checkOutDate;
    private Integer numberOfNights;
    private Double totalPrice;

    public BookingDto() {
    }

    public BookingDto(Booking booking) {
        this.aggregateId = booking.getAggregateId();
        this.version = booking.getVersion();
        this.state = booking.getState();
        this.user = booking.getUser() != null ? new BookingUserDto(booking.getUser()) : null;
        this.room = booking.getRoom() != null ? new BookingRoomDto(booking.getRoom()) : null;
        this.checkInDate = booking.getCheckInDate();
        this.checkOutDate = booking.getCheckOutDate();
        this.numberOfNights = booking.getNumberOfNights();
        this.totalPrice = booking.getTotalPrice();
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

    public BookingUserDto getUser() {
        return user;
    }

    public void setUser(BookingUserDto user) {
        this.user = user;
    }

    public BookingRoomDto getRoom() {
        return room;
    }

    public void setRoom(BookingRoomDto room) {
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