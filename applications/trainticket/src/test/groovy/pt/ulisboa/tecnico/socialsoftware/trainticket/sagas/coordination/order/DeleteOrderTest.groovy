package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.order

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.states.OrderSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.DeleteOrderFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteOrderTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "deleteOrder: success"() {
        // Spec: plan.md §8 Order - DeleteOrder; the soft delete leaves nothing for sagaStateOf to load
        given:
        def orderAggregateId = createOrder()

        when:
        orderFunctionalities.deleteOrder(orderAggregateId)

        and: 'attempt to load the now-deleted order'
        unitOfWorkService.aggregateLoadAndRegisterRead(orderAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "deleteOrder: getOrderStep acquires IN_DELETE_ORDER semantic lock"() {
        // Spec: plan.md §8 Order - DeleteOrder; primary-aggregate lock acquisition
        given:
        def orderAggregateId = createOrder()
        def uow = unitOfWorkService.createUnitOfWork("deleteOrder")
        def func = new DeleteOrderFunctionalitySagas(unitOfWorkService, orderAggregateId, uow, commandGateway)
        func.executeUntilStep("getOrderStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_DELETE_ORDER'
        sagaStateOf(orderAggregateId) == OrderSagaState.IN_DELETE_ORDER

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
