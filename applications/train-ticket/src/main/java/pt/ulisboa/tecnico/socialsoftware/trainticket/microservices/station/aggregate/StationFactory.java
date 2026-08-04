package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

public interface StationFactory {
    Station createStation(Integer aggregateId, StationDto stationDto);
    Station createStationFromExisting(Station existingStation);
    StationDto createStationDto(Station station);
}
