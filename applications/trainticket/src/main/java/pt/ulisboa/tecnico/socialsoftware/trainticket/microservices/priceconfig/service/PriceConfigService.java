package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository;

import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_PRICE_CONFIG;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.PRICE_CONFIG_NOT_FOUND;

@Service
public class PriceConfigService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private PriceConfigFactory priceConfigFactory;

    private final PriceConfigRepository priceConfigRepository;
    private final PriceConfigCustomRepository priceConfigCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public PriceConfigService(UnitOfWorkService unitOfWorkService,
                              PriceConfigRepository priceConfigRepository,
                              PriceConfigCustomRepository priceConfigCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.priceConfigRepository = priceConfigRepository;
        this.priceConfigCustomRepository = priceConfigCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PriceConfigDto getPriceConfigById(Integer priceConfigAggregateId, UnitOfWork unitOfWork) {
        return priceConfigFactory.createPriceConfigDto(
                (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(priceConfigAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<PriceConfigDto> getPriceConfigs(UnitOfWork unitOfWork) {
        List<PriceConfigDto> priceConfigDtos = new ArrayList<>();
        for (PriceConfig priceConfig : priceConfigCustomRepository.findAllLatestActive()) {
            priceConfigDtos.add(priceConfigFactory.createPriceConfigDto(
                    (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(
                            priceConfig.getAggregateId(), unitOfWork)));
        }
        return priceConfigDtos;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PriceConfigDto getPriceConfigByRouteAndTrainType(Integer routeAggregateId,
                                                            Integer trainTypeAggregateId,
                                                            UnitOfWork unitOfWork) {
        Integer aggregateId = priceConfigCustomRepository
                .findLatestActiveByRouteAndTrainType(routeAggregateId, trainTypeAggregateId)
                .map(PriceConfig::getAggregateId)
                .orElseThrow(() -> new TrainticketException(PRICE_CONFIG_NOT_FOUND));

        return priceConfigFactory.createPriceConfigDto(
                (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PriceConfigDto createPriceConfig(PriceConfigDto priceConfigDto, UnitOfWork unitOfWork) {
        checkRouteAndTrainTypePairIsUnique(priceConfigDto.getRouteAggregateId(),
                priceConfigDto.getTrainTypeAggregateId());

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        PriceConfig priceConfig = priceConfigFactory.createPriceConfig(aggregateId, priceConfigDto);

        unitOfWorkService.registerChanged(priceConfig, unitOfWork);
        return priceConfigFactory.createPriceConfigDto(priceConfig);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updatePriceConfig(Integer priceConfigAggregateId, PriceConfigDto priceConfigDto,
                                  UnitOfWork unitOfWork) {
        PriceConfig oldPriceConfig = (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(
                priceConfigAggregateId, unitOfWork);
        PriceConfig newPriceConfig = priceConfigFactory.createPriceConfigCopy(oldPriceConfig);
        newPriceConfig.setBasicPriceRate(priceConfigDto.getBasicPriceRate());
        newPriceConfig.setFirstClassPriceRate(priceConfigDto.getFirstClassPriceRate());

        unitOfWorkService.registerChanged(newPriceConfig, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deletePriceConfig(Integer priceConfigAggregateId, UnitOfWork unitOfWork) {
        PriceConfig oldPriceConfig = (PriceConfig) unitOfWorkService.aggregateLoadAndRegisterRead(
                priceConfigAggregateId, unitOfWork);
        PriceConfig newPriceConfig = priceConfigFactory.createPriceConfigCopy(oldPriceConfig);
        newPriceConfig.remove();

        unitOfWorkService.registerChanged(newPriceConfig, unitOfWork);
    }

    // UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE is a create-only guard: both ids are final, so no
    // update path can break the pair.
    private void checkRouteAndTrainTypePairIsUnique(Integer routeAggregateId, Integer trainTypeAggregateId) {
        for (PriceConfig priceConfig : priceConfigCustomRepository.findAllLatestActive()) {
            if (priceConfig.getRouteAggregateId().equals(routeAggregateId)
                    && priceConfig.getTrainTypeAggregateId().equals(trainTypeAggregateId)) {
                throw new TrainticketException(DUPLICATE_PRICE_CONFIG);
            }
        }
    }
}
