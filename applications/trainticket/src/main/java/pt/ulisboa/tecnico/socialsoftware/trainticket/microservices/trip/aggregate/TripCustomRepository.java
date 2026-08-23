package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import java.util.List;

public interface TripCustomRepository {
    List<Trip> findAllLatestActive();
}
