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
class GetLeftTicketCountTest extends TrainticketSpockTest {

    def "getLeftTicketCount: success"() {
        // Spec: plan.md §8 Order - GetLeftTicketCount; the trip and train type steps supply the capacity
        given: 'two second-class seats held on the departure'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)

        when:
        def result = orderFunctionalities.getLeftTicketCount(tripAggregateId, ORDER_TRAVEL_DATE, ORDER_SEAT_CLASS)

        then: 'the saga resolved the economy capacity of the trip train type and subtracted the held seats'
        result == TRAIN_TYPE_ECONOMY_CLASS_SEATS - 2
    }

    def "getLeftTicketCount: first class capacity is resolved independently of the seats held in economy"() {
        // Spec: plan.md §8 Order - GetLeftTicketCount is scoped to (trip, travel date, seat class)
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)

        when:
        def result = orderFunctionalities.getLeftTicketCount(tripAggregateId, ORDER_TRAVEL_DATE, ORDER_SEAT_CLASS_TWO)

        then:
        result == TRAIN_TYPE_FIRST_CLASS_SEATS
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
