package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderStatus

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CancelOrderCompensationTest extends TrainticketSpockTest {

    def orderAggregateId

    def setup() {
        loadBehaviorScripts()
        orderAggregateId = createOrder()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "cancelOrder: fault on cancelOrderStep compensates the lock acquired by getOrderStep"() {
        // Spec: plan.md §8 Order - CancelOrder; compensate transition
        when:
        orderFunctionalities.cancelOrder(orderAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(orderAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the cancelOrder never ran: read-back shows the pre-saga state'
        def reread = orderFunctionalities.getOrderById(orderAggregateId)
        reread.status == OrderStatus.NOTPAID
        reread.cancelledTime == null
        reread.refundAmount == null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
