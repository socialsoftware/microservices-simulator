package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationFactory;

@Service
public class StationService {
    private final StationCustomRepository stationCustomRepository;
    private final StationFactory stationFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public StationService(StationCustomRepository stationCustomRepository,
                          StationFactory stationFactory,
                          UnitOfWorkService unitOfWorkService,
                          AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.stationCustomRepository = stationCustomRepository;
        this.stationFactory = stationFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StationDto createStation(StationDto stationDto, UnitOfWork unitOfWork) {
        // STATION_NAME_UNIQUE (P3, own-table uniqueness)
        if (stationCustomRepository.findStationIdByName(stationDto.getName()).isPresent()) {
            throw new TrainTicketException(TrainTicketErrorMessage.STATION_NAME_UNIQUE, stationDto.getName());
        }

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Station station = stationFactory.createStation(aggregateId, stationDto.getName(), stationDto.getStayTime());

        unitOfWorkService.registerChanged(station, unitOfWork);
        return stationFactory.createStationDto(station);
    }
}
