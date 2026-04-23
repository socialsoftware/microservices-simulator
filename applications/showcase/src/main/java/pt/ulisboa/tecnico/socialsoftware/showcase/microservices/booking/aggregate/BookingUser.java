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
    private String userName;
    private String userEmail;
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
        setUserName(bookingUserDto.getUsername());
        setUserEmail(bookingUserDto.getEmail());
        setUserAggregateId(bookingUserDto.getAggregateId());
        setUserVersion(bookingUserDto.getVersion());
        setUserState(bookingUserDto.getState() != null ? AggregateState.valueOf(bookingUserDto.getState()) : null);
    }

    public BookingUser(BookingUser other) {
        setUserName(other.getUserName());
        setUserEmail(other.getUserEmail());
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
        dto.setUsername(getUserName());
        dto.setEmail(getUserEmail());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}