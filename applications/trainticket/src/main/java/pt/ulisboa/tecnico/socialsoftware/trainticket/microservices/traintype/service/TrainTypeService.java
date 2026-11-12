package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrainTypeService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private TrainTypeFactory trainTypeFactory;

    private final TrainTypeRepository trainTypeRepository;
    private final TrainTypeCustomRepository trainTypeCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public TrainTypeService(UnitOfWorkService unitOfWorkService,
                            TrainTypeRepository trainTypeRepository,
                            TrainTypeCustomRepository trainTypeCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.trainTypeRepository = trainTypeRepository;
        this.trainTypeCustomRepository = trainTypeCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TrainTypeDto getTrainTypeById(Integer trainTypeAggregateId, UnitOfWork unitOfWork) {
        return trainTypeFactory.createTrainTypeDto(
                (TrainType) unitOfWorkService.aggregateLoadAndRegisterRead(trainTypeAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TrainTypeDto> getTrainTypes(UnitOfWork unitOfWork) {
        List<TrainTypeDto> trainTypes = new ArrayList<>();
        for (TrainType trainType : trainTypeCustomRepository.findAllLatestActive()) {
            trainTypes.add(trainTypeFactory.createTrainTypeDto(
                    (TrainType) unitOfWorkService.aggregateLoadAndRegisterRead(trainType.getAggregateId(), unitOfWork)));
        }
        return trainTypes;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TrainTypeDto createTrainType(TrainTypeDto trainTypeDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        TrainType trainType = trainTypeFactory.createTrainType(aggregateId, trainTypeDto);

        unitOfWorkService.registerChanged(trainType, unitOfWork);
        return trainTypeFactory.createTrainTypeDto(trainType);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTrainType(Integer trainTypeAggregateId, TrainTypeDto trainTypeDto, UnitOfWork unitOfWork) {
        TrainType oldTrainType = (TrainType) unitOfWorkService.aggregateLoadAndRegisterRead(trainTypeAggregateId, unitOfWork);
        TrainType newTrainType = trainTypeFactory.createTrainTypeCopy(oldTrainType);
        newTrainType.setEconomyClassSeats(trainTypeDto.getEconomyClassSeats());
        newTrainType.setFirstClassSeats(trainTypeDto.getFirstClassSeats());
        newTrainType.setAverageSpeed(trainTypeDto.getAverageSpeed());

        unitOfWorkService.registerChanged(newTrainType, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTrainType(Integer trainTypeAggregateId, UnitOfWork unitOfWork) {
        TrainType oldTrainType = (TrainType) unitOfWorkService.aggregateLoadAndRegisterRead(trainTypeAggregateId, unitOfWork);
        TrainType newTrainType = trainTypeFactory.createTrainTypeCopy(oldTrainType);
        newTrainType.remove();

        unitOfWorkService.registerChanged(newTrainType, unitOfWork);
    }
}
