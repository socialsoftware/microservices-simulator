package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.SagaTrip;

@Service
@Profile("sagas")
public class SagasTripFactory implements TripFactory {
    @Override
    public SagaTrip createTrip(Integer aggregateId, TripDto tripDto) {
        return new SagaTrip(aggregateId, tripDto);
    }

    @Override
    public SagaTrip createTripCopy(Trip existing) {
        return new SagaTrip((SagaTrip) existing);
    }

    @Override
    public TripDto createTripDto(Trip trip) {
        return new TripDto(trip);
    }
}
