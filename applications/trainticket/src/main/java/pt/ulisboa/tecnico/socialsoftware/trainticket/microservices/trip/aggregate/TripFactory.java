package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

public interface TripFactory {
    Trip createTrip(Integer aggregateId, TripDto tripDto);

    Trip createTripCopy(Trip existing);

    TripDto createTripDto(Trip trip);
}
