package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetOrderByIdTest extends TrainticketSpockTest {

    def "getOrderById: success"() {
        // Spec: plan.md §8 Order - GetOrderById
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def orderAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)

        when:
        def result = orderFunctionalities.getOrderById(orderAggregateId)

        then: 'the saga returns a coherent DTO'
        result.aggregateId == orderAggregateId
        result.userAggregateId == userAggregateId
        result.contactsAggregateId == contactsAggregateId
        result.tripAggregateId == tripAggregateId
        result.travelDate == ORDER_TRAVEL_DATE
        result.seatClass == ORDER_SEAT_CLASS
        result.price == ORDER_PRICE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
