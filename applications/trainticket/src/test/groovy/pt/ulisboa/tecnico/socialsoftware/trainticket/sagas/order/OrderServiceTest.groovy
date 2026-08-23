package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class OrderServiceTest extends TrainticketSpockTest {

    def "getOrderById: reads back the persisted order through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Order - GetOrderById
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def orderAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)

        when:
        def result = orderService.getOrderById(orderAggregateId,
                unitOfWorkService.createUnitOfWork("getOrderById"))

        then:
        result.aggregateId == orderAggregateId
        result.userAggregateId == userAggregateId
        result.contactsAggregateId == contactsAggregateId
        result.tripAggregateId == tripAggregateId
        result.travelDate == ORDER_TRAVEL_DATE
        result.fromStationName == ORDER_FROM_STATION_NAME
        result.toStationName == ORDER_TO_STATION_NAME
        result.seatClass == ORDER_SEAT_CLASS
        result.contactsName == CONTACTS_NAME
        result.contactsDocumentType == CONTACTS_DOCUMENT_TYPE
        result.contactsDocumentNumber == CONTACTS_DOCUMENT_NUMBER
        result.price == ORDER_PRICE
        result.isActive()
    }

    def "getOrderById: unknown aggregate id"() {
        // Spec: plan.md §8 Order - GetOrderById; Path A (aggregateLoadAndRegisterRead)
        when:
        orderService.getOrderById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getOrderById"))

        then:
        thrown(SimulatorException)
    }

    def "getOrders: returns every persisted order"() {
        // Spec: plan.md §8 Order - GetOrders
        given: 'two orders on the same departure, in different seat classes'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def firstAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        def secondAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId,
                ORDER_TRAVEL_DATE, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME, ORDER_SEAT_CLASS_TWO)

        when:
        def result = orderService.getOrders(unitOfWorkService.createUnitOfWork("getOrders"))

        then:
        result.size() == 2
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.collect { it.seatClass }.toSet() == [ORDER_SEAT_CLASS, ORDER_SEAT_CLASS_TWO].toSet()
    }

    def "getOrders: no order exists"() {
        // Spec: plan.md §8 Order - GetOrders; an empty result is a valid answer
        when:
        def result = orderService.getOrders(unitOfWorkService.createUnitOfWork("getOrders"))

        then:
        result.isEmpty()
    }

    def "getOrdersByAccount: returns only the orders placed by the account"() {
        // Spec: plan.md §8 Order - GetOrdersByAccount
        given: 'two accounts booking on the same departure'
        def tripAggregateId = createBookableTrip()
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def otherUserAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)
        def otherContactsAggregateId = createContacts(otherUserAggregateId, CONTACTS_NAME_TWO,
                CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO)
        def orderAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        createOrder(otherUserAggregateId, otherContactsAggregateId, tripAggregateId)

        when:
        def result = orderService.getOrdersByAccount(userAggregateId,
                unitOfWorkService.createUnitOfWork("getOrdersByAccount"))

        then:
        result.size() == 1
        result[0].aggregateId == orderAggregateId
        result[0].userAggregateId == userAggregateId
        result[0].contactsName == CONTACTS_NAME
    }

    def "getOrdersByAccount: the account placed no order"() {
        // Spec: plan.md §8 Order - GetOrdersByAccount; an empty result is a valid answer
        given:
        def userAggregateId = createUser()

        when:
        def result = orderService.getOrdersByAccount(userAggregateId,
                unitOfWorkService.createUnitOfWork("getOrdersByAccount"))

        then:
        result.isEmpty()
    }

    def "getLeftTicketCount: the seats held on the departure are subtracted from the capacity"() {
        // Spec: plan.md §8 Order - GetLeftTicketCount
        given: 'two second-class seats held on the departure'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)

        when:
        def result = orderService.getLeftTicketCount(tripAggregateId, ORDER_TRAVEL_DATE, ORDER_SEAT_CLASS,
                TRAIN_TYPE_ECONOMY_CLASS_SEATS, unitOfWorkService.createUnitOfWork("getLeftTicketCount"))

        then:
        result == TRAIN_TYPE_ECONOMY_CLASS_SEATS - 2
    }

    def "getLeftTicketCount: seats held in another class do not count against this one"() {
        // Spec: plan.md §8 Order - GetLeftTicketCount is scoped to (trip, travel date, seat class)
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)

        when:
        def result = orderService.getLeftTicketCount(tripAggregateId, ORDER_TRAVEL_DATE, ORDER_SEAT_CLASS_TWO,
                TRAIN_TYPE_FIRST_CLASS_SEATS, unitOfWorkService.createUnitOfWork("getLeftTicketCount"))

        then:
        result == TRAIN_TYPE_FIRST_CLASS_SEATS
    }

    def "getLeftTicketCount: no order holds a seat on the departure"() {
        // Spec: plan.md §8 Order - GetLeftTicketCount; an empty seat set is a valid answer
        given:
        def tripAggregateId = createBookableTrip()

        when:
        def result = orderService.getLeftTicketCount(tripAggregateId, ORDER_TRAVEL_DATE, ORDER_SEAT_CLASS,
                TRAIN_TYPE_ECONOMY_CLASS_SEATS, unitOfWorkService.createUnitOfWork("getLeftTicketCount"))

        then:
        result == TRAIN_TYPE_ECONOMY_CLASS_SEATS
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
