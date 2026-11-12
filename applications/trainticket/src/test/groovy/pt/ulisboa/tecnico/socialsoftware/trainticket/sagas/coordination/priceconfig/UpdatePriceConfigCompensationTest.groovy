package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdatePriceConfigCompensationTest extends TrainticketSpockTest {

    def priceConfigAggregateId

    def setup() {
        loadBehaviorScripts()
        priceConfigAggregateId = createPriceConfig()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updatePriceConfig: fault on updatePriceConfigStep compensates the lock acquired by getPriceConfigStep"() {
        // Spec: plan.md §7 PriceConfig - UpdatePriceConfig; compensate transition
        when:
        priceConfigFunctionalities.updatePriceConfig(priceConfigAggregateId,
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO))

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(priceConfigAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = priceConfigFunctionalities.getPriceConfigById(priceConfigAggregateId)
        reread.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        reread.firstClassPriceRate == PRICE_CONFIG_FIRST_CLASS_RATE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
