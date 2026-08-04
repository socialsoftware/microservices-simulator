package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi.requestDtos.CreateTrainRequestDto;


@Service
@Transactional
public class TrainService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainFactory trainFactory;

    public TrainService() {}

    public TrainDto createTrain(CreateTrainRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TrainDto trainDto = new TrainDto();
            trainDto.setName(createRequest.getName());
            trainDto.setEconomyClass(createRequest.getEconomyClass());
            trainDto.setConfortClass(createRequest.getConfortClass());
            trainDto.setAverageSpeed(createRequest.getAverageSpeed());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Train train = trainFactory.createTrain(aggregateId, trainDto);
            unitOfWorkService.registerChanged(train, unitOfWork);
            return trainFactory.createTrainDto(train);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating train: " + e.getMessage());
        }
    }

    public TrainDto getTrainById(Integer id, UnitOfWork unitOfWork) {
        try {
            Train train = (Train) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return trainFactory.createTrainDto(train);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving train: " + e.getMessage());
        }
    }

    public List<TrainDto> getAllTrains(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = trainRepository.findAll().stream()
                .map(Train::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Train) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(trainFactory::createTrainDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving train: " + e.getMessage());
        }
    }

    public TrainDto updateTrain(TrainDto trainDto, UnitOfWork unitOfWork) {
        try {
            Integer id = trainDto.getAggregateId();
            Train oldTrain = (Train) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Train newTrain = trainFactory.createTrainFromExisting(oldTrain);
            if (trainDto.getName() != null) {
                newTrain.setName(trainDto.getName());
            }
            if (trainDto.getEconomyClass() != null) {
                newTrain.setEconomyClass(trainDto.getEconomyClass());
            }
            if (trainDto.getConfortClass() != null) {
                newTrain.setConfortClass(trainDto.getConfortClass());
            }
            if (trainDto.getAverageSpeed() != null) {
                newTrain.setAverageSpeed(trainDto.getAverageSpeed());
            }

            unitOfWorkService.registerChanged(newTrain, unitOfWork);            TrainUpdatedEvent event = new TrainUpdatedEvent(newTrain.getAggregateId(), newTrain.getName(), newTrain.getEconomyClass(), newTrain.getConfortClass(), newTrain.getAverageSpeed());
            event.setPublisherAggregateVersion(newTrain.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return trainFactory.createTrainDto(newTrain);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating train: " + e.getMessage());
        }
    }

    public void deleteTrain(Integer id, UnitOfWork unitOfWork) {
        try {
            Train oldTrain = (Train) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Train newTrain = trainFactory.createTrainFromExisting(oldTrain);
            newTrain.remove();
            unitOfWorkService.registerChanged(newTrain, unitOfWork);            unitOfWorkService.registerEvent(new TrainDeletedEvent(newTrain.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting train: " + e.getMessage());
        }
    }








}