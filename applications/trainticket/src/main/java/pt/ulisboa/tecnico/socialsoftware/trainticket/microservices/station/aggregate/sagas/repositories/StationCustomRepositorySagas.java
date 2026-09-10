package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationRepository;

import java.util.List;

@Service
@Profile("sagas")
public class StationCustomRepositorySagas implements StationCustomRepository {
    @Autowired
    private StationRepository stationRepository;

    @Override
    public List<Station> findAllLatestActive() {
        return stationRepository.findAllLatestActive();
    }
}
