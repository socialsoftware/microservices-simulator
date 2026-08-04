package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi.requestDtos.CreateStationRequestDto;


@Service
@Transactional
public class StationService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private StationFactory stationFactory;

    public StationService() {}

    public StationDto createStation(CreateStationRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            StationDto stationDto = new StationDto();
            stationDto.setName(createRequest.getName());
            stationDto.setStayTime(createRequest.getStayTime());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Station station = stationFactory.createStation(aggregateId, stationDto);
            unitOfWorkService.registerChanged(station, unitOfWork);
            return stationFactory.createStationDto(station);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating station: " + e.getMessage());
        }
    }

    public StationDto getStationById(Integer id, UnitOfWork unitOfWork) {
        try {
            Station station = (Station) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return stationFactory.createStationDto(station);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving station: " + e.getMessage());
        }
    }

    public List<StationDto> getAllStations(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = stationRepository.findAll().stream()
                .map(Station::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Station) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(stationFactory::createStationDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving station: " + e.getMessage());
        }
    }

    public StationDto updateStation(StationDto stationDto, UnitOfWork unitOfWork) {
        try {
            Integer id = stationDto.getAggregateId();
            Station oldStation = (Station) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Station newStation = stationFactory.createStationFromExisting(oldStation);
            if (stationDto.getName() != null) {
                newStation.setName(stationDto.getName());
            }
            if (stationDto.getStayTime() != null) {
                newStation.setStayTime(stationDto.getStayTime());
            }

            unitOfWorkService.registerChanged(newStation, unitOfWork);            StationUpdatedEvent event = new StationUpdatedEvent(newStation.getAggregateId(), newStation.getName(), newStation.getStayTime());
            event.setPublisherAggregateVersion(newStation.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return stationFactory.createStationDto(newStation);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating station: " + e.getMessage());
        }
    }

    public void deleteStation(Integer id, UnitOfWork unitOfWork) {
        try {
            Station oldStation = (Station) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Station newStation = stationFactory.createStationFromExisting(oldStation);
            newStation.remove();
            unitOfWorkService.registerChanged(newStation, unitOfWork);            unitOfWorkService.registerEvent(new StationDeletedEvent(newStation.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting station: " + e.getMessage());
        }
    }








}