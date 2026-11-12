package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import java.util.List;

public interface StationCustomRepository {
    List<Station> findAllLatestActive();
}
