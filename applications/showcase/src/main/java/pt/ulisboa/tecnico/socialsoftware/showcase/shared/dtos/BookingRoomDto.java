package pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRoom;

public class BookingRoomDto implements Serializable {
    private String roomNumber;
    private Double pricePerNight;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public BookingRoomDto() {
    }

    public BookingRoomDto(BookingRoom bookingRoom) {
        this.roomNumber = bookingRoom.getRoomNumber();
        this.pricePerNight = bookingRoom.getPricePerNight();
        this.aggregateId = bookingRoom.getRoomAggregateId();
        this.version = bookingRoom.getRoomVersion();
        this.state = bookingRoom.getRoomState() != null ? bookingRoom.getRoomState().name() : null;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}