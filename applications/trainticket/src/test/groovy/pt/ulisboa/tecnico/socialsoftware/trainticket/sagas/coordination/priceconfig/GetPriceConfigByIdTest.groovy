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
class GetPriceConfigByIdTest extends TrainticketSpockTest {

    def "getPriceConfigById: success"() {
        // Spec: plan.md §7 PriceConfig - GetPriceConfigById
        given: 'a price config exists'
        def priceConfigAggregateId = createPriceConfig()

        when:
        def result = priceConfigFunctionalities.getPriceConfigById(priceConfigAggregateId)

        then: 'the saga returns a coherent price config DTO'
        result.aggregateId == priceConfigAggregateId
        result.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        result.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        result.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        result.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
