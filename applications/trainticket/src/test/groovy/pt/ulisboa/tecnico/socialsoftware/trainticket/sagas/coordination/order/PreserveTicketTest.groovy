package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderStatus

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.CONTACTS_BELONG_TO_ACCOUNT
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ENDPOINTS_ON_TRIP_ROUTE
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.PRICE_CONFIG_NOT_FOUND
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.SEAT_CAPACITY_NOT_EXCEEDED

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class PreserveTicketTest extends TrainticketSpockTest {

    // A departure whose requested class holds exactly one seat, so the capacity guard is reachable
    // through the saga without booking out a full cabin.
    private Integer singleFirstClassSeatTrip() {
        def routeAggregateId = createRoute()
        def trainTypeAggregateId = createTrainType(TRAIN_TYPE_NAME, TRAIN_TYPE_ECONOMY_CLASS_SEATS,
                TRAIN_TYPE_SEATS_ONE, TRAIN_TYPE_AVERAGE_SPEED)
        createPriceConfig(routeAggregateId, trainTypeAggregateId)
        return createTrip(TRIP_NUMBER, routeAggregateId, trainTypeAggregateId)
    }

    def "preserveTicket: success"() {
        // Spec: plan.md §8 Order - PreserveTicket
        given: 'an account, its passenger contact and a bookable departure'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()

        when:
        def result = orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId,
                tripAggregateId, ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME,
                ORDER_SEAT_CLASS)

        then: 'orchestration outcome only - persistence is asserted in T2'
        result.aggregateId != null
        result.status == OrderStatus.NOTPAID
        result.tripAggregateId == tripAggregateId
        result.contactsAggregateId == contactsAggregateId
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "preserveTicket: TRIP_EXISTS violation - the trip does not exist"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule TRIP_EXISTS, P4a enforced by getTripStep
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)

        when:
        orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId,
                NONEXISTENT_AGGREGATE_ID, ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME,
                ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS)

        then:
        thrown(SimulatorException)
    }

    def "preserveTicket: CONTACTS_EXIST violation - the passenger contact does not exist"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule CONTACTS_EXIST, P4a enforced by getContactsStep
        given:
        def userAggregateId = createUser()
        def tripAggregateId = createBookableTrip()

        when:
        orderFunctionalities.preserveTicket(userAggregateId, NONEXISTENT_AGGREGATE_ID, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS)

        then:
        thrown(SimulatorException)
    }

    def "preserveTicket: PRICE_CONFIG_EXISTS violation - the route and train type have no tariff"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule PRICE_CONFIG_EXISTS, P4a enforced by the
        // getPriceConfigStep lookup, which reports the miss with the upstream's own constant
        given: 'a trip whose (route, train type) pair was never priced'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createTrip()

        when:
        orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS)

        then:
        def ex = thrown(TrainticketException)
        ex.message == PRICE_CONFIG_NOT_FOUND
    }

    def "preserveTicket: CONTACTS_BELONG_TO_ACCOUNT violation"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule CONTACTS_BELONG_TO_ACCOUNT, P3 over the
        // ContactsDto the saga assembled
        given:
        def userAggregateId = createUser()
        def otherUserAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)
        def contactsAggregateId = createContacts(otherUserAggregateId)
        def tripAggregateId = createBookableTrip()

        when:
        orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS)

        then:
        def ex = thrown(TrainticketException)
        ex.message == CONTACTS_BELONG_TO_ACCOUNT
    }

    def "preserveTicket: ENDPOINTS_ON_TRIP_ROUTE violation"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule ENDPOINTS_ON_TRIP_ROUTE, P3 over the RouteDto
        // the saga assembled
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()

        when:
        orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_UNKNOWN_STATION_NAME, ORDER_SEAT_CLASS)

        then:
        def ex = thrown(TrainticketException)
        ex.message == ENDPOINTS_ON_TRIP_ROUTE
    }

    def "preserveTicket: SEAT_CAPACITY_NOT_EXCEEDED violation"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule SEAT_CAPACITY_NOT_EXCEEDED, P3 over the
        // capacity scalar the getTrainTypeStep supplied
        given: 'the one first-class seat on the departure is already held'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = singleFirstClassSeatTrip()
        orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS_TWO)

        when:
        orderFunctionalities.preserveTicket(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS_TWO)

        then:
        def ex = thrown(TrainticketException)
        ex.message == SEAT_CAPACITY_NOT_EXCEEDED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
