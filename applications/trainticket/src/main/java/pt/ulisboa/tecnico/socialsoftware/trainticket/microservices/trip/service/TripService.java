package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;

import java.util.ArrayList;
import java.util.List;

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
}
