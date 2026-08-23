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
class GetOrdersTest extends TrainticketSpockTest {

    def "getOrders: success"() {
        // Spec: plan.md §8 Order - GetOrders
        given: 'two orders on the same departure, in different seat classes'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def firstAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        def secondAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS_TWO)

        when:
        def result = orderFunctionalities.getOrders()

        then: 'the saga returns a coherent DTO per order'
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.seatClass }.toSet() == [ORDER_SEAT_CLASS, ORDER_SEAT_CLASS_TWO].toSet()
        result.every { it.tripAggregateId == tripAggregateId }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
