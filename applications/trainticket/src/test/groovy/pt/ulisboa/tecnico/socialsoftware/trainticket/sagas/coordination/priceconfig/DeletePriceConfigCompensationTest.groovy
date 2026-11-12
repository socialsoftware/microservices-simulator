package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.priceconfig

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeletePriceConfigCompensationTest extends TrainticketSpockTest {

    def priceConfigAggregateId

    def setup() {
        loadBehaviorScripts()
        priceConfigAggregateId = createPriceConfig()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deletePriceConfig: fault on deletePriceConfigStep compensates the lock acquired by getPriceConfigStep"() {
        // Spec: plan.md §7 PriceConfig - DeletePriceConfig; compensate transition
        when:
        priceConfigFunctionalities.deletePriceConfig(priceConfigAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(priceConfigAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the delete never ran: read-back shows the pre-saga state'
        def reread = priceConfigFunctionalities.getPriceConfigById(priceConfigAggregateId)
        reread.basicPriceRate == PRICE_CONFIG_BASIC_RATE
        reread.isActive()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
