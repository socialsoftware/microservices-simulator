package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.PRICE_CONFIG_NOT_FOUND

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class PriceConfigServiceTest extends TrainticketSpockTest {

    def "getPriceConfigById: reads back the persisted price config through a fresh UnitOfWork"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigById
        given:
        def priceConfigAggregateId = createPriceConfig()

        when:
        def result = priceConfigService.getPriceConfigById(priceConfigAggregateId,
                unitOfWorkService.createUnitOfWork("getPriceConfigById"))

        then:
        result.aggregateId == priceConfigAggregateId
        result.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        result.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        result.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        result.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
        result.isActive()
    }

    def "getPriceConfigById: unknown aggregate id"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigById; Path A (aggregateLoadAndRegisterRead)
        when:
        priceConfigService.getPriceConfigById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getPriceConfigById"))

        then:
        thrown(SimulatorException)
    }

    def "getPriceConfigs: returns every persisted price config"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigs
        given:
        def firstAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        def secondAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO)

        when:
        def result = priceConfigService.getPriceConfigs(
                unitOfWorkService.createUnitOfWork("getPriceConfigs"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        def second = result.find { it.aggregateId == secondAggregateId }
        first.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        first.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        first.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        first.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
        second.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO
        second.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO
        second.basicPriceRate == PRICE_CONFIG_BASIC_RATE_TWO
        second.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE_TWO
    }

    def "getPriceConfigs: no price config exists"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigs; an empty result is a valid answer
        when:
        def result = priceConfigService.getPriceConfigs(
                unitOfWorkService.createUnitOfWork("getPriceConfigs"))

        then:
        result.isEmpty()
    }

    def "getPriceConfigByRouteAndTrainType: reads back the configuration of the pair"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigByRouteAndTrainType
        given: 'two configurations differing in the route and train type they configure'
        def priceConfigAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO)

        when:
        def result = priceConfigService.getPriceConfigByRouteAndTrainType(
                PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getPriceConfigByRouteAndTrainType"))

        then:
        result.aggregateId == priceConfigAggregateId
        result.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        result.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        result.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        result.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
    }

    def "getPriceConfigByRouteAndTrainType: no configuration exists for the pair"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigByRouteAndTrainType; Path B (custom repository)
        given: 'a configuration exists for a different train type on the same route'
        createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)

        when:
        priceConfigService.getPriceConfigByRouteAndTrainType(
                PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getPriceConfigByRouteAndTrainType"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == PRICE_CONFIG_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
