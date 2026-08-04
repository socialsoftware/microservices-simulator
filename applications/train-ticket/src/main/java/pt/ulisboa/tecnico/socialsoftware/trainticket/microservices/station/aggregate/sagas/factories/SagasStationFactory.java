package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.SagaStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.dtos.SagaStationDto;

@Service
@Profile("sagas")
public class SagasStationFactory implements StationFactory {
    @Override
    public Station createStation(Integer aggregateId, StationDto stationDto) {
        return new SagaStation(aggregateId, stationDto);
    }

    @Override
    public Station createStationFromExisting(Station existingStation) {
        return new SagaStation((SagaStation) existingStation);
    }

    @Override
    public StationDto createStationDto(Station station) {
        return new SagaStationDto(station);
    }
}