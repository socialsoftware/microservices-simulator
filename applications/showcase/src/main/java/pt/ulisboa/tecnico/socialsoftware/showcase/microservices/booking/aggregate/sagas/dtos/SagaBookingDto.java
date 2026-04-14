package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.SagaBooking;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaBookingDto extends BookingDto {
private SagaState sagaState;

public SagaBookingDto(Booking booking) {
super((Booking) booking);
this.sagaState = ((SagaBooking)booking).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}