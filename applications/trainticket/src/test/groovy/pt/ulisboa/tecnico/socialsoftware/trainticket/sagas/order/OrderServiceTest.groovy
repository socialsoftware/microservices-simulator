package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderStatus
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass

import java.time.LocalDate

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.CONTACTS_BELONG_TO_ACCOUNT
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ENDPOINTS_ON_TRIP_ROUTE
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_STATUS_TRANSITION
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.SEAT_CAPACITY_NOT_EXCEEDED

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
        result.price == ORDER_BOOKED_PRICE
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

    // The DTOs PreserveTicketFunctionalitySagas assembles before it calls the service, gathered here
    // so the T2 cases invoke the service method directly, without the workflow.
    private Map bookingContext(Integer tripAggregateId, Integer contactsAggregateId) {
        def tripDto = tripFunctionalities.getTripById(tripAggregateId)
        return [contacts   : contactsFunctionalities.getContactsById(contactsAggregateId),
                trip       : tripDto,
                route      : routeFunctionalities.getRouteById(tripDto.routeAggregateId),
                priceConfig: priceConfigFunctionalities.getPriceConfigByRouteAndTrainType(
                        tripDto.routeAggregateId, tripDto.trainTypeAggregateId)]
    }

    private static OrderDto bookingRequest(Integer userAggregateId, Integer contactsAggregateId,
                                           Integer tripAggregateId,
                                           SeatClass seatClass = ORDER_SEAT_CLASS,
                                           String fromStationName = ORDER_FROM_STATION_NAME,
                                           String toStationName = ORDER_TO_STATION_NAME,
                                           LocalDate travelDate = ORDER_TRAVEL_DATE) {
        def orderDto = new OrderDto()
        orderDto.setUserAggregateId(userAggregateId)
        orderDto.setContactsAggregateId(contactsAggregateId)
        orderDto.setTripAggregateId(tripAggregateId)
        orderDto.setTravelDate(travelDate)
        orderDto.setFromStationName(fromStationName)
        orderDto.setToStationName(toStationName)
        orderDto.setSeatClass(seatClass)
        return orderDto
    }

    def "preserveTicket: the booked terms are persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Order - PreserveTicket postconditions; rules PRICE_MATCHES_TARIFF,
        // DEPARTURE_TIME_MATCHES_TRIP, SEAT_NUMBER_UNIQUE_PER_DEPARTURE
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def context = bookingContext(tripAggregateId, contactsAggregateId)

        when:
        def result = orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_ECONOMY_CLASS_SEATS, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = orderService.getOrderById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.status == OrderStatus.NOTPAID
        readBack.price == ORDER_BOOKED_PRICE
        readBack.departureTime == ORDER_DEPARTURE_TIME
        readBack.seatNumber == ORDER_SEAT_NUMBER_ON_POINT
        readBack.tripNumber == TRIP_NUMBER
        readBack.contactsName == CONTACTS_NAME
        readBack.contactsDocumentType == CONTACTS_DOCUMENT_TYPE
        readBack.contactsDocumentNumber == CONTACTS_DOCUMENT_NUMBER
        readBack.boughtDate != null
        !readBack.boughtDate.isAfter(readBack.departureTime)
        readBack.refundAmount == null
        readBack.cancelledTime == null
        readBack.isActive()
    }

    def "preserveTicket: the first-class fare is charged at the first-class rate"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule PRICE_MATCHES_TARIFF, the seat-class branch
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def context = bookingContext(tripAggregateId, contactsAggregateId)

        when:
        def result = orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId, ORDER_SEAT_CLASS_TWO),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_FIRST_CLASS_SEATS, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then:
        result.price == ORDER_BOOKED_PRICE_FIRST_CLASS
        result.seatClass == ORDER_SEAT_CLASS_TWO
    }

    def "preserveTicket: the allocated seat is the lowest one no live order holds"() {
        // Spec: plan.md §8 Order - PreserveTicket allocates the lowest free seat in [1, capacity];
        // rule SEAT_NUMBER_UNIQUE_PER_DEPARTURE
        given: 'two seats taken on the departure, the first of them then released by a cancellation'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def firstAggregateId = createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        createOrder(userAggregateId, contactsAggregateId, tripAggregateId)
        orderService.cancelOrder(firstAggregateId, unitOfWorkService.createUnitOfWork("cancelOrder"))
        def context = bookingContext(tripAggregateId, contactsAggregateId)

        when:
        def result = orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_ECONOMY_CLASS_SEATS, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then: 'the cancelled order released seat 1, so the new booking reuses it'
        result.seatNumber == 1
    }

    def "preserveTicket: CONTACTS_BELONG_TO_ACCOUNT violation"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule CONTACTS_BELONG_TO_ACCOUNT
        given: 'a contact owned by a different account'
        def userAggregateId = createUser()
        def otherUserAggregateId = createUser(USER_NAME_TWO, USER_PASSWORD_TWO, USER_GENDER_TWO,
                USER_DOCUMENT_TYPE_TWO, USER_DOCUMENT_NUMBER_TWO, USER_EMAIL_TWO)
        def contactsAggregateId = createContacts(otherUserAggregateId)
        def tripAggregateId = createBookableTrip()
        def context = bookingContext(tripAggregateId, contactsAggregateId)

        when:
        orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_ECONOMY_CLASS_SEATS, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == CONTACTS_BELONG_TO_ACCOUNT
    }

    def "preserveTicket: ENDPOINTS_ON_TRIP_ROUTE violation - #description"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule ENDPOINTS_ON_TRIP_ROUTE
        given:
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def context = bookingContext(tripAggregateId, contactsAggregateId)

        when:
        orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId, ORDER_SEAT_CLASS,
                        fromStationName, toStationName),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_ECONOMY_CLASS_SEATS, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == ENDPOINTS_ON_TRIP_ROUTE

        where:
        description                          | fromStationName             | toStationName
        "the origin is not on the route"     | ORDER_UNKNOWN_STATION_NAME  | ORDER_TO_STATION_NAME
        "the destination is not on the route"| ORDER_FROM_STATION_NAME     | ORDER_UNKNOWN_STATION_NAME
        "the journey runs against the route" | ORDER_TO_STATION_NAME       | ORDER_FROM_STATION_NAME
    }

    def "preserveTicket: SEAT_CAPACITY_NOT_EXCEEDED on-point - the last free seat is bookable"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule SEAT_CAPACITY_NOT_EXCEEDED, last satisfying count
        given: 'a first-class cabin of one seat, still empty'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def context = bookingContext(tripAggregateId, contactsAggregateId)

        when:
        def result = orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId, ORDER_SEAT_CLASS_TWO),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_SEATS_ONE, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then:
        notThrown(TrainticketException)
        result.seatNumber == ORDER_SEAT_NUMBER_ON_POINT
    }

    def "preserveTicket: SEAT_CAPACITY_NOT_EXCEEDED off-point - the cabin is full"() {
        // Spec: plan.md §8 Order - PreserveTicket; rule SEAT_CAPACITY_NOT_EXCEEDED, first violating count
        given: 'the one first-class seat on the departure is already held'
        def userAggregateId = createUser()
        def contactsAggregateId = createContacts(userAggregateId)
        def tripAggregateId = createBookableTrip()
        def context = bookingContext(tripAggregateId, contactsAggregateId)
        orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId, ORDER_SEAT_CLASS_TWO),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_SEATS_ONE, unitOfWorkService.createUnitOfWork("preserveTicket"))

        when:
        orderService.preserveTicket(
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId, ORDER_SEAT_CLASS_TWO),
                context.contacts, context.trip, context.route, context.priceConfig,
                TRAIN_TYPE_SEATS_ONE, unitOfWorkService.createUnitOfWork("preserveTicket"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == SEAT_CAPACITY_NOT_EXCEEDED
    }

    def "payOrder: the order moves to PAID and the change is readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Order - PayOrder
        given:
        def orderAggregateId = createOrder()

        when:
        orderService.payOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("payOrder"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = orderService.getOrderById(orderAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.status == OrderStatus.PAID
        readBack.refundAmount == null
        readBack.cancelledTime == null
    }

    def "collectTicket: a paid order moves to COLLECTED"() {
        // Spec: plan.md §8 Order - CollectTicket
        given:
        def orderAggregateId = createOrder()
        orderService.payOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("payOrder"))

        when:
        orderService.collectTicket(orderAggregateId, unitOfWorkService.createUnitOfWork("collectTicket"))

        then:
        def readBack = orderService.getOrderById(orderAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.status == OrderStatus.COLLECTED
    }

    def "useTicket: a collected order moves to USED"() {
        // Spec: plan.md §8 Order - UseTicket
        given:
        def orderAggregateId = createOrder()
        orderService.payOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("payOrder"))
        orderService.collectTicket(orderAggregateId, unitOfWorkService.createUnitOfWork("collectTicket"))

        when:
        orderService.useTicket(orderAggregateId, unitOfWorkService.createUnitOfWork("useTicket"))

        then:
        def readBack = orderService.getOrderById(orderAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.status == OrderStatus.USED
    }

    def "collectTicket: an unpaid order cannot be collected"() {
        // Spec: plan.md §8 Order - CollectTicket; rule ORDER_STATUS_TRANSITION has no NOTPAID -> COLLECTED edge
        given:
        def orderAggregateId = createOrder()

        when:
        orderService.collectTicket(orderAggregateId, unitOfWorkService.createUnitOfWork("collectTicket"))

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_STATUS_TRANSITION
    }

    def "cancelOrder: cancelling an unpaid order refunds nothing"() {
        // Spec: plan.md §8 Order - CancelOrder; rule ORDER_REFUND_AMOUNT, the NOTPAID branch
        given:
        def orderAggregateId = createOrder()

        when:
        orderService.cancelOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("cancelOrder"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = orderService.getOrderById(orderAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.status == OrderStatus.CANCELLED
        readBack.refundAmount == ORDER_REFUND_NONE
        readBack.cancelledTime != null
    }

    def "cancelOrder: cancelling a paid order before departure refunds eighty percent of the fare"() {
        // Spec: plan.md §8 Order - CancelOrder; rule ORDER_REFUND_AMOUNT, the paid branch
        given:
        def orderAggregateId = createOrder()
        orderService.payOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("payOrder"))

        when:
        orderService.cancelOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("cancelOrder"))

        then:
        def readBack = orderService.getOrderById(orderAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.status == OrderStatus.CANCELLED
        readBack.refundAmount == ORDER_BOOKED_REFUND_PAID
        readBack.cancelledTime.isBefore(readBack.departureTime)
    }

    def "deleteOrder: the order is soft-deleted and no longer loadable"() {
        // Spec: plan.md §8 Order - DeleteOrder
        given:
        def orderAggregateId = createOrder()

        when:
        orderService.deleteOrder(orderAggregateId, unitOfWorkService.createUnitOfWork("deleteOrder"))

        and: 'attempt to load the now-deleted order'
        orderService.getOrderById(orderAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "#serviceMethod: unknown aggregate id"() {
        // Spec: plan.md §8 Order - the write methods load by id; Path A (aggregateLoadAndRegisterRead)
        when:
        orderService."$serviceMethod"(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork(serviceMethod as String))

        then:
        thrown(SimulatorException)

        where:
        serviceMethod << ["payOrder", "collectTicket", "useTicket", "cancelOrder", "deleteOrder"]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
