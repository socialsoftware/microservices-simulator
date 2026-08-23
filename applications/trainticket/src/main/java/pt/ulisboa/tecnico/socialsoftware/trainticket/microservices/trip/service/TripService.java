package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;

import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_TRIP_NUMBER;

@Service
public class TripService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private TripFactory tripFactory;

    private final TripRepository tripRepository;
    private final TripCustomRepository tripCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public TripService(UnitOfWorkService unitOfWorkService,
                       TripRepository tripRepository,
                       TripCustomRepository tripCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.tripRepository = tripRepository;
        this.tripCustomRepository = tripCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TripDto getTripById(Integer tripAggregateId, UnitOfWork unitOfWork) {
        return tripFactory.createTripDto(
                (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(tripAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TripDto> getTrips(UnitOfWork unitOfWork) {
        List<TripDto> tripDtos = new ArrayList<>();
        for (Trip trip : tripCustomRepository.findAllLatestActive()) {
            tripDtos.add(tripFactory.createTripDto(
                    (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(trip.getAggregateId(), unitOfWork)));
        }
        return tripDtos;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TripDto createTrip(TripDto tripDto, UnitOfWork unitOfWork) {
        checkTripNumberIsUnique(tripDto.getTripNumber());

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Trip trip = tripFactory.createTrip(aggregateId, tripDto);

        unitOfWorkService.registerChanged(trip, unitOfWork);
        return tripFactory.createTripDto(trip);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTrip(Integer tripAggregateId, TripDto tripDto, UnitOfWork unitOfWork) {
        Trip oldTrip = (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(tripAggregateId, unitOfWork);
        Trip newTrip = tripFactory.createTripCopy(oldTrip);
        newTrip.setStartTime(tripDto.getStartTime());
        newTrip.setEndTime(tripDto.getEndTime());

        unitOfWorkService.registerChanged(newTrip, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTrip(Integer tripAggregateId, UnitOfWork unitOfWork) {
        Trip oldTrip = (Trip) unitOfWorkService.aggregateLoadAndRegisterRead(tripAggregateId, unitOfWork);
        Trip newTrip = tripFactory.createTripCopy(oldTrip);
        newTrip.remove();

        unitOfWorkService.registerChanged(newTrip, unitOfWork);
    }

    // UNIQUE_TRIP_NUMBER is a create-only guard: tripNumber is final, so no update path can break it.
    private void checkTripNumberIsUnique(String tripNumber) {
        for (Trip trip : tripCustomRepository.findAllLatestActive()) {
            if (trip.getTripNumber().equals(tripNumber)) {
                throw new TrainticketException(DUPLICATE_TRIP_NUMBER);
            }
        }
    }
}
