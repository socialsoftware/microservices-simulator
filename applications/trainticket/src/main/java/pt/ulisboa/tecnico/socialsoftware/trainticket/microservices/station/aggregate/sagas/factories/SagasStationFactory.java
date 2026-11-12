package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.SagaStation;

@Service
@Profile("sagas")
public class SagasStationFactory implements StationFactory {
    @Override
    public SagaStation createStation(Integer aggregateId, StationDto stationDto) {
        return new SagaStation(aggregateId, stationDto);
    }

    @Override
    public SagaStation createStationCopy(Station existing) {
        return new SagaStation((SagaStation) existing);
    }

    @Override
    public StationDto createStationDto(Station station) {
        return new StationDto(station);
    }
}
