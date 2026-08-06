package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigRouteDto;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.PriceConfigDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.PriceConfigUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi.requestDtos.CreatePriceConfigRequestDto;


@Service
@Transactional
public class PriceConfigService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private PriceConfigRepository priceconfigRepository;

    @Autowired
    private PriceConfigFactory priceconfigFactory;

    public PriceConfigService() {}

    public PriceConfigDto createPriceConfig(CreatePriceConfigRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            PriceConfigDto priceconfigDto = new PriceConfigDto();
            priceconfigDto.setBasicPriceRate(createRequest.getBasicPriceRate());
            priceconfigDto.setFirstClassPriceRate(createRequest.getFirstClassPriceRate());
            if (createRequest.getTrainType() != null) {
                PriceConfigTrainDto trainTypeDto = new PriceConfigTrainDto();
                trainTypeDto.setAggregateId(createRequest.getTrainType().getAggregateId());
                trainTypeDto.setVersion(createRequest.getTrainType().getVersion());
                trainTypeDto.setState(createRequest.getTrainType().getState() != null ? createRequest.getTrainType().getState().name() : null);
                priceconfigDto.setTrainType(trainTypeDto);
            }
            if (createRequest.getRoute() != null) {
                PriceConfigRouteDto routeDto = new PriceConfigRouteDto();
                routeDto.setAggregateId(createRequest.getRoute().getAggregateId());
                routeDto.setVersion(createRequest.getRoute().getVersion());
                routeDto.setState(createRequest.getRoute().getState() != null ? createRequest.getRoute().getState().name() : null);
                priceconfigDto.setRoute(routeDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            PriceConfig priceconfig = priceconfigFactory.createPriceConfig(aggregateId, priceconfigDto);
            unitOfWorkService.registerChanged(priceconfig, unitOfWork);
            return priceconfigFactory.createPriceConfigDto(priceconfig);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating priceconfig: " + e.getMessage());
        }
    }

    public PriceConfigDto getPriceConfigById(Integer id, UnitOfWork unitOfWork) {
        try {
            PriceConfig priceconfig = (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return priceconfigFactory.createPriceConfigDto(priceconfig);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving priceconfig: " + e.getMessage());
        }
    }

    public List<PriceConfigDto> getAllPriceConfigs(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = priceconfigRepository.findAll().stream()
                .map(PriceConfig::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(priceconfigFactory::createPriceConfigDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving priceconfig: " + e.getMessage());
        }
    }

    public PriceConfigDto updatePriceConfig(PriceConfigDto priceconfigDto, UnitOfWork unitOfWork) {
        try {
            Integer id = priceconfigDto.getAggregateId();
            PriceConfig oldPriceConfig = (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            PriceConfig newPriceConfig = priceconfigFactory.createPriceConfigFromExisting(oldPriceConfig);
            if (priceconfigDto.getBasicPriceRate() != null) {
                newPriceConfig.setBasicPriceRate(priceconfigDto.getBasicPriceRate());
            }
            if (priceconfigDto.getFirstClassPriceRate() != null) {
                newPriceConfig.setFirstClassPriceRate(priceconfigDto.getFirstClassPriceRate());
            }

            unitOfWorkService.registerChanged(newPriceConfig, unitOfWork);            PriceConfigUpdatedEvent event = new PriceConfigUpdatedEvent(newPriceConfig.getAggregateId(), newPriceConfig.getBasicPriceRate(), newPriceConfig.getFirstClassPriceRate());
            event.setPublisherAggregateVersion(newPriceConfig.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return priceconfigFactory.createPriceConfigDto(newPriceConfig);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating priceconfig: " + e.getMessage());
        }
    }

    public void deletePriceConfig(Integer id, UnitOfWork unitOfWork) {
        try {
            PriceConfig oldPriceConfig = (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            PriceConfig newPriceConfig = priceconfigFactory.createPriceConfigFromExisting(oldPriceConfig);
            newPriceConfig.remove();
            unitOfWorkService.registerChanged(newPriceConfig, unitOfWork);            unitOfWorkService.registerEvent(new PriceConfigDeletedEvent(newPriceConfig.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting priceconfig: " + e.getMessage());
        }
    }








}