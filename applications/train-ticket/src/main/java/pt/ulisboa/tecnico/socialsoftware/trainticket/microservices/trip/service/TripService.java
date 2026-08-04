package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripRouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripStartStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripTerminalStationDto;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.TripType;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TripDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TripUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi.requestDtos.CreateTripRequestDto;


@Service
@Transactional
public class TripService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripFactory tripFactory;

    public TripService() {}

    public TripDto createTrip(CreateTripRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TripDto tripDto = new TripDto();
            tripDto.setTripType(createRequest.getTripType() != null ? createRequest.getTripType().name() : null);
            tripDto.setTripNumber(createRequest.getTripNumber());
            tripDto.setStartTime(createRequest.getStartTime());
            tripDto.setEndTime(createRequest.getEndTime());
            if (createRequest.getTrainType() != null) {
                TripTrainDto trainTypeDto = new TripTrainDto();
                trainTypeDto.setAggregateId(createRequest.getTrainType().getAggregateId());
                trainTypeDto.setVersion(createRequest.getTrainType().getVersion());
                trainTypeDto.setState(createRequest.getTrainType().getState() != null ? createRequest.getTrainType().getState().name() : null);
                tripDto.setTrainType(trainTypeDto);
            }
            if (createRequest.getRoute() != null) {
                TripRouteDto routeDto = new TripRouteDto();
                routeDto.setAggregateId(createRequest.getRoute().getAggregateId());
                routeDto.setVersion(createRequest.getRoute().getVersion());
                routeDto.setState(createRequest.getRoute().getState() != null ? createRequest.getRoute().getState().name() : null);
                tripDto.setRoute(routeDto);
            }
            if (createRequest.getStartStation() != null) {
                TripStartStationDto startStationDto = new TripStartStationDto();
                startStationDto.setAggregateId(createRequest.getStartStation().getAggregateId());
                startStationDto.setVersion(createRequest.getStartStation().getVersion());
                startStationDto.setState(createRequest.getStartStation().getState() != null ? createRequest.getStartStation().getState().name() : null);
                tripDto.setStartStation(startStationDto);
            }
            if (createRequest.getTerminalStation() != null) {
                TripTerminalStationDto terminalStationDto = new TripTerminalStationDto();
                terminalStationDto.setAggregateId(createRequest.getTerminalStation().getAggregateId());
                terminalStationDto.setVersion(createRequest.getTerminalStation().getVersion());
                terminalStationDto.setState(createRequest.getTerminalStation().getState() != null ? createRequest.getTerminalStation().getState().name() : null);
                tripDto.setTerminalStation(terminalStationDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Trip trip = tripFactory.createTrip(aggregateId, tripDto);
            unitOfWorkService.registerChanged(trip, unitOfWork);
            return tripFactory.createTripDto(trip);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating trip: " + e.getMessage());
        }
    }

    public TripDto getTripById(Integer id, UnitOfWork unitOfWork) {
        try {
            Trip trip = (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return tripFactory.createTripDto(trip);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving trip: " + e.getMessage());
        }
    }

    public List<TripDto> getAllTrips(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = tripRepository.findAll().stream()
                .map(Trip::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(tripFactory::createTripDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving trip: " + e.getMessage());
        }
    }

    public TripDto updateTrip(TripDto tripDto, UnitOfWork unitOfWork) {
        try {
            Integer id = tripDto.getAggregateId();
            Trip oldTrip = (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Trip newTrip = tripFactory.createTripFromExisting(oldTrip);
            if (tripDto.getTripType() != null) {
                newTrip.setTripType(TripType.valueOf(tripDto.getTripType()));
            }
            if (tripDto.getTripNumber() != null) {
                newTrip.setTripNumber(tripDto.getTripNumber());
            }
            if (tripDto.getStartTime() != null) {
                newTrip.setStartTime(tripDto.getStartTime());
            }
            if (tripDto.getEndTime() != null) {
                newTrip.setEndTime(tripDto.getEndTime());
            }

            unitOfWorkService.registerChanged(newTrip, unitOfWork);            TripUpdatedEvent event = new TripUpdatedEvent(newTrip.getAggregateId(), newTrip.getTripNumber(), newTrip.getStartTime(), newTrip.getEndTime());
            event.setPublisherAggregateVersion(newTrip.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return tripFactory.createTripDto(newTrip);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating trip: " + e.getMessage());
        }
    }

    public void deleteTrip(Integer id, UnitOfWork unitOfWork) {
        try {
            Trip oldTrip = (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Trip newTrip = tripFactory.createTripFromExisting(oldTrip);
            newTrip.remove();
            unitOfWorkService.registerChanged(newTrip, unitOfWork);            unitOfWorkService.registerEvent(new TripDeletedEvent(newTrip.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting trip: " + e.getMessage());
        }
    }








}