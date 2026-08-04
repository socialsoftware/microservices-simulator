package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.SagaTrip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.dtos.SagaTripDto;

@Service
@Profile("sagas")
public class SagasTripFactory implements TripFactory {
    @Override
    public Trip createTrip(Integer aggregateId, TripDto tripDto) {
        return new SagaTrip(aggregateId, tripDto);
    }

    @Override
    public Trip createTripFromExisting(Trip existingTrip) {
        return new SagaTrip((SagaTrip) existingTrip);
    }

    @Override
    public TripDto createTripDto(Trip trip) {
        return new SagaTripDto(trip);
    }
}