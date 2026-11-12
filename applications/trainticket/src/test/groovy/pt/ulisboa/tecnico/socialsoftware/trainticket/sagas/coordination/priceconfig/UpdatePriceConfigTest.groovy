package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.priceconfig

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.states.PriceConfigSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.UpdatePriceConfigFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdatePriceConfigTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "updatePriceConfig: success"() {
        // Spec: plan.md §7 PriceConfig - UpdatePriceConfig
        given: 'a price config exists'
        def priceConfigAggregateId = createPriceConfig()

        when:
        priceConfigFunctionalities.updatePriceConfig(priceConfigAggregateId,
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO))

        then: 'the traversal completes and releases the lock'
        sagaStateOf(priceConfigAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updatePriceConfig: getPriceConfigStep acquires IN_UPDATE_PRICE_CONFIG semantic lock"() {
        // Spec: plan.md §7 PriceConfig - UpdatePriceConfig; primary-aggregate lock acquisition
        given:
        def priceConfigAggregateId = createPriceConfig()
        def uow = unitOfWorkService.createUnitOfWork("updatePriceConfig")
        def func = new UpdatePriceConfigFunctionalitySagas(unitOfWorkService, priceConfigAggregateId,
                new PriceConfigDto(PRICE_CONFIG_ROUTE_AGGREGATE_ID, PRICE_CONFIG_TRAIN_TYPE_AGGREGATE_ID,
                        PRICE_CONFIG_BASIC_RATE_TWO, PRICE_CONFIG_FIRST_CLASS_RATE_TWO),
                uow, commandGateway)
        func.executeUntilStep("getPriceConfigStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_PRICE_CONFIG'
        sagaStateOf(priceConfigAggregateId) == PriceConfigSagaState.IN_UPDATE_PRICE_CONFIG

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
