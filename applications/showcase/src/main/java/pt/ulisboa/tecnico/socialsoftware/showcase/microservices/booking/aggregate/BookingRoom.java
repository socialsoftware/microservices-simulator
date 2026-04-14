package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingRoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;

@Entity
public class BookingRoom {
    @Id
    @GeneratedValue
    private Long id;
    private String roomNumber;
    private Double pricePerNight;
    private Integer roomAggregateId;
    private Integer roomVersion;
    private AggregateState roomState;
    @OneToOne
    private Booking booking;

    public BookingRoom() {

    }

    public BookingRoom(RoomDto roomDto) {
        setRoomAggregateId(roomDto.getAggregateId());
        setRoomVersion(roomDto.getVersion());
        setRoomState(roomDto.getState());
    }

    public BookingRoom(BookingRoomDto bookingRoomDto) {
        setRoomNumber(bookingRoomDto.getRoomNumber());
        setPricePerNight(bookingRoomDto.getPricePerNight());
        setRoomAggregateId(bookingRoomDto.getAggregateId());
        setRoomVersion(bookingRoomDto.getVersion());
        setRoomState(bookingRoomDto.getState() != null ? AggregateState.valueOf(bookingRoomDto.getState()) : null);
    }

    public BookingRoom(BookingRoom other) {
        setRoomNumber(other.getRoomNumber());
        setPricePerNight(other.getPricePerNight());
        setRoomAggregateId(other.getRoomAggregateId());
        setRoomVersion(other.getRoomVersion());
        setRoomState(other.getRoomState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getRoomAggregateId() {
        return roomAggregateId;
    }

    public void setRoomAggregateId(Integer roomAggregateId) {
        this.roomAggregateId = roomAggregateId;
    }

    public Integer getRoomVersion() {
        return roomVersion;
    }

    public void setRoomVersion(Integer roomVersion) {
        this.roomVersion = roomVersion;
    }

    public AggregateState getRoomState() {
        return roomState;
    }

    public void setRoomState(AggregateState roomState) {
        this.roomState = roomState;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }




    public BookingRoomDto buildDto() {
        BookingRoomDto dto = new BookingRoomDto();
        dto.setRoomNumber(getRoomNumber());
        dto.setPricePerNight(getPricePerNight());
        dto.setAggregateId(getRoomAggregateId());
        dto.setVersion(getRoomVersion());
        dto.setState(getRoomState() != null ? getRoomState().name() : null);
        return dto;
    }
}