package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;

import java.util.List;

@Service
@Profile("sagas")
public class TripCustomRepositorySagas implements TripCustomRepository {
    @Autowired
    private TripRepository tripRepository;

    @Override
    public List<Trip> findAllLatestActive() {
        return tripRepository.findAllLatestActive();
    }
}
