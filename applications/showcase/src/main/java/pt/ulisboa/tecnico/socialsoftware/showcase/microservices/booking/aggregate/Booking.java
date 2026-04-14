package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.subscribe.BookingSubscribesUserDeletedUserMustExist;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingRoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingUserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.PaymentMethod;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Booking extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "booking")
    private BookingUser user;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "booking")
    private BookingRoom room;
    private String checkInDate;
    private String checkOutDate;
    private Integer numberOfNights;
    private Double totalPrice;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private Boolean confirmed;

    public Booking() {
        this.paymentMethod = PaymentMethod.CREDIT_CARD;
    }

    public Booking(Integer aggregateId, BookingDto bookingDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setCheckInDate(bookingDto.getCheckInDate());
        setCheckOutDate(bookingDto.getCheckOutDate());
        setNumberOfNights(bookingDto.getNumberOfNights());
        setTotalPrice(bookingDto.getTotalPrice());
        setPaymentMethod(bookingDto.getPaymentMethod() != null ? PaymentMethod.valueOf(bookingDto.getPaymentMethod()) : PaymentMethod.CREDIT_CARD);
        setConfirmed(bookingDto.getConfirmed());
        setUser(bookingDto.getUser() != null ? new BookingUser(bookingDto.getUser()) : null);
        setRoom(bookingDto.getRoom() != null ? new BookingRoom(bookingDto.getRoom()) : null);
    }


    public Booking(Booking other) {
        super(other);
        setUser(other.getUser() != null ? new BookingUser(other.getUser()) : null);
        setRoom(other.getRoom() != null ? new BookingRoom(other.getRoom()) : null);
        setCheckInDate(other.getCheckInDate());
        setCheckOutDate(other.getCheckOutDate());
        setNumberOfNights(other.getNumberOfNights());
        setTotalPrice(other.getTotalPrice());
        setPaymentMethod(other.getPaymentMethod());
        setConfirmed(other.getConfirmed());
    }

    public BookingUser getUser() {
        return user;
    }

    public void setUser(BookingUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setBooking(this);
        }
    }

    public BookingRoom getRoom() {
        return room;
    }

    public void setRoom(BookingRoom room) {
        this.room = room;
        if (this.room != null) {
            this.room.setBooking(this);
        }
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantUserMustExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantUserMustExist(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new BookingSubscribesUserDeletedUserMustExist(this.getUser()));
    }


    private boolean invariantRule0() {
        return this.checkInDate != null && this.checkInDate.length() > 0;
    }

    private boolean invariantRule1() {
        return this.checkOutDate != null && this.checkOutDate.length() > 0;
    }

    private boolean invariantRule2() {
        return numberOfNights > 0;
    }

    private boolean invariantRule3() {
        return totalPrice > 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Check-in date is required");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Check-out date is required");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "Number of nights must be positive");
        }
        if (!invariantRule3()) {
            throw new SimulatorException(INVARIANT_BREAK, "Total price must be positive");
        }
    }

    public BookingDto buildDto() {
        BookingDto dto = new BookingDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUser(getUser() != null ? new BookingUserDto(getUser()) : null);
        dto.setRoom(getRoom() != null ? new BookingRoomDto(getRoom()) : null);
        dto.setCheckInDate(getCheckInDate());
        dto.setCheckOutDate(getCheckOutDate());
        dto.setNumberOfNights(getNumberOfNights());
        dto.setTotalPrice(getTotalPrice());
        dto.setPaymentMethod(getPaymentMethod() != null ? getPaymentMethod().name() : null);
        dto.setConfirmed(getConfirmed());
        return dto;
    }
}