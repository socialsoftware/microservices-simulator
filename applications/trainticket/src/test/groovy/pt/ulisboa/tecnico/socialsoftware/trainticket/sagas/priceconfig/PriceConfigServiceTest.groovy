package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_PRICE_CONFIG
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

    def "createPriceConfig: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §7 PriceConfig - CreatePriceConfig postconditions
        when:
        def result = priceConfigService.createPriceConfig(
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE),
                unitOfWorkService.createUnitOfWork("createPriceConfig"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = priceConfigService.getPriceConfigById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        readBack.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        readBack.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        readBack.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
        readBack.isActive()
    }

    def "createPriceConfig: DUPLICATE_PRICE_CONFIG violation"() {
        // Spec: plan.md §7 PriceConfig - rule UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE (P3, own table, create only)
        given: 'a configuration already covers the route and train type'
        createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID)

        when:
        priceConfigService.createPriceConfig(
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO),
                unitOfWorkService.createUnitOfWork("createPriceConfig"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == DUPLICATE_PRICE_CONFIG
    }

    def "createPriceConfig: the pair is free again once the holding configuration is deleted"() {
        // Spec: plan.md §7 PriceConfig - rule UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE (P3, own table, create only)
        given: 'the configuration covering the pair has been deleted'
        def deletedAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID)
        priceConfigService.deletePriceConfig(deletedAggregateId,
                unitOfWorkService.createUnitOfWork("deletePriceConfig"))

        when:
        def result = priceConfigService.createPriceConfig(
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO),
                unitOfWorkService.createUnitOfWork("createPriceConfig"))

        then: 'the freed pair is accepted'
        notThrown(TrainticketException)
        priceConfigService.getPriceConfigById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check")).basicPriceRate == PRICE_CONFIG_BASIC_RATE_TWO
    }

    def "createPriceConfig: the same route is configurable for a second train type"() {
        // Spec: plan.md §7 PriceConfig - rule UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE constrains the pair, not either id
        given:
        createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID)

        when:
        def result = priceConfigService.createPriceConfig(
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO),
                unitOfWorkService.createUnitOfWork("createPriceConfig"))

        then:
        notThrown(TrainticketException)
        priceConfigService.getPriceConfigById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check")).trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO
    }

    def "updatePriceConfig: new rates persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §7 PriceConfig - UpdatePriceConfig postconditions
        given:
        def priceConfigAggregateId = createPriceConfig()

        when:
        priceConfigService.updatePriceConfig(priceConfigAggregateId,
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO),
                unitOfWorkService.createUnitOfWork("updatePriceConfig"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = priceConfigService.getPriceConfigById(priceConfigAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.basicPriceRate == PRICE_CONFIG_BASIC_RATE_TWO
        readBack.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE_TWO
        readBack.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        readBack.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
    }

    def "updatePriceConfig: unknown aggregate id"() {
        // Spec: plan.md §7 PriceConfig - UpdatePriceConfig; Path A (aggregateLoadAndRegisterRead)
        when:
        priceConfigService.updatePriceConfig(NONEXISTENT_AGGREGATE_ID,
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO),
                unitOfWorkService.createUnitOfWork("updatePriceConfig"))

        then:
        thrown(SimulatorException)
    }

    def "deletePriceConfig: the deleted price config is no longer loadable"() {
        // Spec: plan.md §7 PriceConfig - DeletePriceConfig (soft delete)
        given:
        def priceConfigAggregateId = createPriceConfig()

        when:
        priceConfigService.deletePriceConfig(priceConfigAggregateId,
                unitOfWorkService.createUnitOfWork("deletePriceConfig"))

        and: 'read back through a second, fresh UnitOfWork'
        priceConfigService.getPriceConfigById(priceConfigAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "deletePriceConfig: unknown aggregate id"() {
        // Spec: plan.md §7 PriceConfig - DeletePriceConfig; Path A (aggregateLoadAndRegisterRead)
        when:
        priceConfigService.deletePriceConfig(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deletePriceConfig"))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
