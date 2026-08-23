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
}
