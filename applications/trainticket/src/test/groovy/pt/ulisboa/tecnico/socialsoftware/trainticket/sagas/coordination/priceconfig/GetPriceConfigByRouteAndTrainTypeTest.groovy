package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetPriceConfigByRouteAndTrainTypeTest extends TrainticketSpockTest {

    def "getPriceConfigByRouteAndTrainType: success"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigByRouteAndTrainType
        given: 'two price configs exist, one per route and train type pair'
        def priceConfigAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO)

        when:
        def result = priceConfigFunctionalities.getPriceConfigByRouteAndTrainType(
                PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID)

        then: 'the saga returns the DTO of the configuration for the pair'
        result.aggregateId == priceConfigAggregateId
        result.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        result.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        result.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        result.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
