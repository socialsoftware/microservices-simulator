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
class GetPriceConfigsTest extends TrainticketSpockTest {

    def "getPriceConfigs: success"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigs
        given: 'two price configs exist'
        def firstAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE)
        def secondAggregateId = createPriceConfig(PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID_TWO,
                PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO)

        when:
        def result = priceConfigFunctionalities.getPriceConfigs()

        then: 'the saga returns a coherent DTO per price config'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.routeAggregateId }.toSet() ==
                [PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_ROUTE_AGGREGATE_ID_TWO].toSet()
        result.collect { it.basicPriceRate }.toSet() ==
                [PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_BASIC_RATE_TWO].toSet()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
