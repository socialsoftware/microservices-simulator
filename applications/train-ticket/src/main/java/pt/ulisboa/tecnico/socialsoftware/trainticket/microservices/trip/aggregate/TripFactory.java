package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;

public interface TripFactory {
    Trip createTrip(Integer aggregateId, TripDto tripDto);
    Trip createTripFromExisting(Trip existingTrip);
    TripDto createTripDto(Trip trip);
}
