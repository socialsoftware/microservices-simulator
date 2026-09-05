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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_STATION_NAME;

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StationDto createStation(StationDto stationDto, UnitOfWork unitOfWork) {
        checkNameIsUnique(stationDto.getName(), null);

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Station station = stationFactory.createStation(aggregateId, stationDto);

        unitOfWorkService.registerChanged(station, unitOfWork);
        return stationFactory.createStationDto(station);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateStation(Integer stationAggregateId, StationDto stationDto, UnitOfWork unitOfWork) {
        checkNameIsUnique(stationDto.getName(), stationAggregateId);

        Station oldStation = (Station) unitOfWorkService.aggregateLoadAndRegisterRead(stationAggregateId, unitOfWork);
        Station newStation = stationFactory.createStationCopy(oldStation);
        newStation.setName(stationDto.getName());
        newStation.setStayTime(stationDto.getStayTime());

        unitOfWorkService.registerChanged(newStation, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteStation(Integer stationAggregateId, UnitOfWork unitOfWork) {
        Station oldStation = (Station) unitOfWorkService.aggregateLoadAndRegisterRead(stationAggregateId, unitOfWork);
        Station newStation = stationFactory.createStationCopy(oldStation);
        newStation.remove();

        unitOfWorkService.registerChanged(newStation, unitOfWork);
    }

    private void checkNameIsUnique(String name, Integer stationAggregateIdUnderChange) {
        for (Station station : stationCustomRepository.findAllLatestActive()) {
            if (station.getAggregateId().equals(stationAggregateIdUnderChange)) {
                continue;
            }
            if (station.getName().equals(name)) {
                throw new TrainticketException(DUPLICATE_STATION_NAME);
            }
        }
    }
}
