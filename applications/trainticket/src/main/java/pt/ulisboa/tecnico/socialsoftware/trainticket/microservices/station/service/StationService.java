package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class StationService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private StationFactory stationFactory;

    private final StationRepository stationRepository;
    private final StationCustomRepository stationCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public StationService(UnitOfWorkService unitOfWorkService,
                          StationRepository stationRepository,
                          StationCustomRepository stationCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.stationRepository = stationRepository;
        this.stationCustomRepository = stationCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StationDto getStationById(Integer stationAggregateId, UnitOfWork unitOfWork) {
        return stationFactory.createStationDto(
                (Station) unitOfWorkService.aggregateLoadAndRegisterRead(stationAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<StationDto> getStations(UnitOfWork unitOfWork) {
        List<StationDto> stations = new ArrayList<>();
        for (Station station : stationCustomRepository.findAllLatestActive()) {
            stations.add(stationFactory.createStationDto(
                    (Station) unitOfWorkService.aggregateLoadAndRegisterRead(station.getAggregateId(), unitOfWork)));
        }
        return stations;
    }
}
