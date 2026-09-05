package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.priceconfig

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.states.PriceConfigSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.DeletePriceConfigFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeletePriceConfigTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    // No happy-path case: deletePriceConfig makes its own aggregate unresolvable, so sagaStateOf throws.
    // See docs/concepts/testing.md § T4 - "Exception — a functionality whose success makes
    // its own aggregate unresolvable". The delete's effect is asserted in PriceConfigServiceTest (T2).

    def "deletePriceConfig: getPriceConfigStep acquires IN_DELETE_PRICE_CONFIG semantic lock"() {
        // Spec: plan.md §7 PriceConfig - DeletePriceConfig; primary-aggregate lock acquisition
        given:
        def priceConfigAggregateId = createPriceConfig()
        def uow = unitOfWorkService.createUnitOfWork("deletePriceConfig")
        def func = new DeletePriceConfigFunctionalitySagas(unitOfWorkService, priceConfigAggregateId,
                uow, commandGateway)
        func.executeUntilStep("getPriceConfigStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_PRICE_CONFIG'
        sagaStateOf(priceConfigAggregateId) == PriceConfigSagaState.IN_DELETE_PRICE_CONFIG

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
