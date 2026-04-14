package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingUserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;

@Entity
public class BookingUser {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String email;
    private Integer userAggregateId;
    private Integer userVersion;
    private AggregateState userState;
    @OneToOne
    private Booking booking;

    public BookingUser() {

    }

    public BookingUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public BookingUser(BookingUserDto bookingUserDto) {
        setUsername(bookingUserDto.getUsername());
        setEmail(bookingUserDto.getEmail());
        setUserAggregateId(bookingUserDto.getAggregateId());
        setUserVersion(bookingUserDto.getVersion());
        setUserState(bookingUserDto.getState() != null ? AggregateState.valueOf(bookingUserDto.getState()) : null);
    }

    public BookingUser(BookingUser other) {
        setUsername(other.getUsername());
        setEmail(other.getEmail());
        setUserAggregateId(other.getUserAggregateId());
        setUserVersion(other.getUserVersion());
        setUserState(other.getUserState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Integer userVersion) {
        this.userVersion = userVersion;
    }

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }




    public BookingUserDto buildDto() {
        BookingUserDto dto = new BookingUserDto();
        dto.setUsername(getUsername());
        dto.setEmail(getEmail());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}