package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.OrderStatus;

public class CreateOrderRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private ContactsDto contacts;
    @NotNull
    private TripDto trip;
    @NotNull
    private TrainDto train;
    @NotNull
    private StationDto fromStation;
    @NotNull
    private StationDto toStation;
    @NotNull
    private String boughtDate;
    @NotNull
    private String travelDate;
    @NotNull
    private String travelTime;
    @NotNull
    private Integer coachNumber;
    @NotNull
    private SeatClass seatClass;
    @NotNull
    private String seatNumber;
    @NotNull
    private Double price;
    @NotNull
    private OrderStatus status;

    public CreateOrderRequestDto() {}

    public CreateOrderRequestDto(UserDto user, ContactsDto contacts, TripDto trip, TrainDto train, StationDto fromStation, StationDto toStation, String boughtDate, String travelDate, String travelTime, Integer coachNumber, SeatClass seatClass, String seatNumber, Double price, OrderStatus status) {
        this.user = user;
        this.contacts = contacts;
        this.trip = trip;
        this.train = train;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.boughtDate = boughtDate;
        this.travelDate = travelDate;
        this.travelTime = travelTime;
        this.coachNumber = coachNumber;
        this.seatClass = seatClass;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public ContactsDto getContacts() {
        return contacts;
    }

    public void setContacts(ContactsDto contacts) {
        this.contacts = contacts;
    }
    public TripDto getTrip() {
        return trip;
    }

    public void setTrip(TripDto trip) {
        this.trip = trip;
    }
    public TrainDto getTrain() {
        return train;
    }

    public void setTrain(TrainDto train) {
        this.train = train;
    }
    public StationDto getFromStation() {
        return fromStation;
    }

    public void setFromStation(StationDto fromStation) {
        this.fromStation = fromStation;
    }
    public StationDto getToStation() {
        return toStation;
    }

    public void setToStation(StationDto toStation) {
        this.toStation = toStation;
    }
    public String getBoughtDate() {
        return boughtDate;
    }

    public void setBoughtDate(String boughtDate) {
        this.boughtDate = boughtDate;
    }
    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }
    public String getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(String travelTime) {
        this.travelTime = travelTime;
    }
    public Integer getCoachNumber() {
        return coachNumber;
    }

    public void setCoachNumber(Integer coachNumber) {
        this.coachNumber = coachNumber;
    }
    public SeatClass getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }
    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
