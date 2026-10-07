package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import java.util.Optional;

public interface StationCustomRepository {
    Optional<Integer> findStationIdByName(String name);
}
