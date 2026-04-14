package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;

@Entity
public class SagaBooking extends Booking implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.showcase.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaBooking() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaBooking(SagaBooking other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaBooking(Integer aggregateId, BookingDto bookingDto) {
        super(aggregateId, bookingDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}