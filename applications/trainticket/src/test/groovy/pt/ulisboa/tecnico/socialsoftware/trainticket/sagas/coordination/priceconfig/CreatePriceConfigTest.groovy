package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreatePriceConfigTest extends TrainticketSpockTest {

    def "createPriceConfig: success"() {
        // Spec: plan.md §7 PriceConfig - CreatePriceConfig
        when:
        def result = priceConfigFunctionalities.createPriceConfig(
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE, PRICE_CONFIG_FIRST_CLASS_RATE))

        then: 'the saga returns a coherent price config DTO'
        result.aggregateId != null
        result.routeAggregateId == PRICE_CONFIG_ROUTE_AGGREGATE_ID
        result.trainTypeAggregateId == PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID
        result.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        result.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
