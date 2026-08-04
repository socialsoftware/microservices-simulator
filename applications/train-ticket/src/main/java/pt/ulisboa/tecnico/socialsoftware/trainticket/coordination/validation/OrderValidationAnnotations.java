package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderContacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderFromStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderToStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderTrain;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderTrip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderUser;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.SeatClass;

public class OrderValidationAnnotations {

    public static class BoughtDateValidation {
        @NotNull
    @NotBlank
        private String boughtDate;
        
        public String getBoughtDate() {
            return boughtDate;
        }
        
        public void setBoughtDate(String boughtDate) {
            this.boughtDate = boughtDate;
        }
    }

    public static class TravelDateValidation {
        @NotNull
    @NotBlank
        private String travelDate;
        
        public String getTravelDate() {
            return travelDate;
        }
        
        public void setTravelDate(String travelDate) {
            this.travelDate = travelDate;
        }
    }

    public static class TravelTimeValidation {
        @NotNull
    @NotBlank
        private String travelTime;
        
        public String getTravelTime() {
            return travelTime;
        }
        
        public void setTravelTime(String travelTime) {
            this.travelTime = travelTime;
        }
    }

    public static class CoachNumberValidation {
        @NotNull
        private Integer coachNumber;
        
        public Integer getCoachNumber() {
            return coachNumber;
        }
        
        public void setCoachNumber(Integer coachNumber) {
            this.coachNumber = coachNumber;
        }
    }

    public static class SeatClassValidation {
        @NotNull
        private SeatClass seatClass;
        
        public SeatClass getSeatClass() {
            return seatClass;
        }
        
        public void setSeatClass(SeatClass seatClass) {
            this.seatClass = seatClass;
        }
    }

    public static class SeatNumberValidation {
        @NotNull
    @NotBlank
        private String seatNumber;
        
        public String getSeatNumber() {
            return seatNumber;
        }
        
        public void setSeatNumber(String seatNumber) {
            this.seatNumber = seatNumber;
        }
    }

    public static class PriceValidation {
        @NotNull
        private Double price;
        
        public Double getPrice() {
            return price;
        }
        
        public void setPrice(Double price) {
            this.price = price;
        }
    }

    public static class StatusValidation {
        @NotNull
        private OrderStatus status;
        
        public OrderStatus getStatus() {
            return status;
        }
        
        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }

    public static class UserValidation {
        @NotNull
        private OrderUser user;
        
        public OrderUser getUser() {
            return user;
        }
        
        public void setUser(OrderUser user) {
            this.user = user;
        }
    }

    public static class ContactsValidation {
        @NotNull
        private OrderContacts contacts;
        
        public OrderContacts getContacts() {
            return contacts;
        }
        
        public void setContacts(OrderContacts contacts) {
            this.contacts = contacts;
        }
    }

    public static class TripValidation {
        @NotNull
        private OrderTrip trip;
        
        public OrderTrip getTrip() {
            return trip;
        }
        
        public void setTrip(OrderTrip trip) {
            this.trip = trip;
        }
    }

    public static class TrainValidation {
        @NotNull
        private OrderTrain train;
        
        public OrderTrain getTrain() {
            return train;
        }
        
        public void setTrain(OrderTrain train) {
            this.train = train;
        }
    }

    public static class FromStationValidation {
        @NotNull
        private OrderFromStation fromStation;
        
        public OrderFromStation getFromStation() {
            return fromStation;
        }
        
        public void setFromStation(OrderFromStation fromStation) {
            this.fromStation = fromStation;
        }
    }

    public static class ToStationValidation {
        @NotNull
        private OrderToStation toStation;
        
        public OrderToStation getToStation() {
            return toStation;
        }
        
        public void setToStation(OrderToStation toStation) {
            this.toStation = toStation;
        }
    }

}