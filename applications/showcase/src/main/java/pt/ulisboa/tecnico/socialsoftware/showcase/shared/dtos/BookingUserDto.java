package pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingUser;

public class BookingUserDto implements Serializable {
    private String username;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public BookingUserDto() {
    }

    public BookingUserDto(BookingUser bookingUser) {
        this.username = bookingUser.getUsername();
        this.email = bookingUser.getEmail();
        this.aggregateId = bookingUser.getUserAggregateId();
        this.version = bookingUser.getUserVersion();
        this.state = bookingUser.getUserState() != null ? bookingUser.getUserState().name() : null;
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