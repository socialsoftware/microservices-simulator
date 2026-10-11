package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeFactory;

@Service
public class TrainTypeService {
    private final TrainTypeCustomRepository trainTypeCustomRepository;
    private final TrainTypeFactory trainTypeFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public TrainTypeService(TrainTypeCustomRepository trainTypeCustomRepository,
                            TrainTypeFactory trainTypeFactory,
                            UnitOfWorkService unitOfWorkService,
                            AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.trainTypeCustomRepository = trainTypeCustomRepository;
        this.trainTypeFactory = trainTypeFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TrainTypeDto createTrainType(TrainTypeDto trainTypeDto, UnitOfWork unitOfWork) {
        // TRAIN_TYPE_NAME_UNIQUE (P3, own-table uniqueness)
        if (trainTypeCustomRepository.findTrainTypeIdByName(trainTypeDto.getName()).isPresent()) {
            throw new TrainTicketException(TrainTicketErrorMessage.TRAIN_TYPE_NAME_UNIQUE, trainTypeDto.getName());
        }

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        TrainType trainType = trainTypeFactory.createTrainType(aggregateId, trainTypeDto.getName(),
                trainTypeDto.getEconomyClassSeats(), trainTypeDto.getFirstClassSeats(),
                trainTypeDto.getAverageSpeed());

        unitOfWorkService.registerChanged(trainType, unitOfWork);
        return trainTypeFactory.createTrainTypeDto(trainType);
    }
}
