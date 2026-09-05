package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.order

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderStatus
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.states.OrderSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.CancelOrderFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CancelOrderTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "cancelOrder: success"() {
        // Spec: plan.md §8 Order - CancelOrder
        given: 'an order in the state the operation moves out of'
        def orderAggregateId = createOrder()

        when:
        orderFunctionalities.cancelOrder(orderAggregateId)

        then: 'the traversal completes and releases the lock'
        sagaStateOf(orderAggregateId) == GenericSagaState.NOT_IN_SAGA
        orderFunctionalities.getOrderById(orderAggregateId).status == OrderStatus.CANCELLED
    }

    def "cancelOrder: getOrderStep acquires IN_CANCEL_ORDER semantic lock"() {
        // Spec: plan.md §8 Order - CancelOrder; primary-aggregate lock acquisition
        given:
        def orderAggregateId = createOrder()
        def uow = unitOfWorkService.createUnitOfWork("cancelOrder")
        def func = new CancelOrderFunctionalitySagas(unitOfWorkService, orderAggregateId, uow, commandGateway)
        func.executeUntilStep("getOrderStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_CANCEL_ORDER'
        sagaStateOf(orderAggregateId) == OrderSagaState.IN_CANCEL_ORDER

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
