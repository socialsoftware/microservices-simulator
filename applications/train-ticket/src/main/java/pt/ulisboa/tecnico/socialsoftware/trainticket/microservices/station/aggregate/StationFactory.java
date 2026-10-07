package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

public interface StationFactory {
    Station createStation(Integer aggregateId, String name, Integer stayTime);

    Station createStationCopy(Station existing);

    StationDto createStationDto(Station station);
}
