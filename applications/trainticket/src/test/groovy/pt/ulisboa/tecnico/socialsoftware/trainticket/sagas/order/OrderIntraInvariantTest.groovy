package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.order

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderStatus
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.SagaOrder

import java.time.LocalDateTime

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_CANCELLATION_FIELDS_SET
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_DEPARTURE_AFTER_PURCHASE
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_PRICE_POSITIVE
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_REFUND_AMOUNT
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_SEAT_NUMBER_POSITIVE
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_STATUS_TRANSITION

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class OrderIntraInvariantTest extends TrainticketSpockTest {

    private static SagaOrder orderWith(BigDecimal price = ORDER_PRICE,
                                       Integer seatNumber = ORDER_SEAT_NUMBER,
                                       LocalDateTime boughtDate = ORDER_BOUGHT_DATE,
                                       LocalDateTime departureTime = ORDER_DEPARTURE_TIME) {
        return new SagaOrder(1, new OrderDto(ORDER_TRIP_AGGREGATE_ID, ORDER_CONTACTS_AGGREGATE_ID,
                ORDER_USER_AGGREGATE_ID, boughtDate, ORDER_TRAVEL_DATE, departureTime,
                ORDER_TRIP_NUMBER, ORDER_FROM_STATION_NAME, ORDER_TO_STATION_NAME,
                ORDER_SEAT_CLASS, seatNumber, ORDER_CONTACTS_NAME, ORDER_CONTACTS_DOCUMENT_TYPE,
                ORDER_CONTACTS_DOCUMENT_NUMBER, price))
    }

    // Copy-on-write: the next version of an order, chained onto the one it supersedes. The chain is
    // what ORDER_STATUS_TRANSITION and ORDER_REFUND_AMOUNT predicate on.
    private static SagaOrder nextVersionOf(SagaOrder previous) {
        def next = new SagaOrder(previous)
        next.setPrev(previous)
        return next
    }

    private static SagaOrder orderAt(OrderStatus status) {
        def order = nextVersionOf(orderWith())
        order.setStatus(status)
        return order
    }

    private static SagaOrder cancellationOf(SagaOrder previous,
                                            BigDecimal refundAmount,
                                            LocalDateTime cancelledTime = ORDER_CANCELLED_TIME) {
        def cancelled = nextVersionOf(previous)
        cancelled.setStatus(OrderStatus.CANCELLED)
        cancelled.setCancelledTime(cancelledTime)
        cancelled.setRefundAmount(refundAmount)
        return cancelled
    }

    def "create order"() {
        // Spec: plan.md §8 Order - field list (the frozen contract terms plus status, refundAmount, cancelledTime)
        when:
        def order = orderWith()
        order.verifyInvariants()

        then:
        order.aggregateId == 1
        order.tripAggregateId == ORDER_TRIP_AGGREGATE_ID
        order.contactsAggregateId == ORDER_CONTACTS_AGGREGATE_ID
        order.userAggregateId == ORDER_USER_AGGREGATE_ID
        order.boughtDate == ORDER_BOUGHT_DATE
        order.travelDate == ORDER_TRAVEL_DATE
        order.departureTime == ORDER_DEPARTURE_TIME
        order.tripNumber == ORDER_TRIP_NUMBER
        order.fromStationName == ORDER_FROM_STATION_NAME
        order.toStationName == ORDER_TO_STATION_NAME
        order.seatClass == ORDER_SEAT_CLASS
        order.seatNumber == ORDER_SEAT_NUMBER
        order.contactsName == ORDER_CONTACTS_NAME
        order.contactsDocumentType == ORDER_CONTACTS_DOCUMENT_TYPE
        order.contactsDocumentNumber == ORDER_CONTACTS_DOCUMENT_NUMBER
        order.price == ORDER_PRICE
        order.status == OrderStatus.NOTPAID
        order.refundAmount == null
        order.cancelledTime == null
        order.state == Aggregate.AggregateState.ACTIVE
        order.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "order: ORDER_STATUS_TRANSITION violation - the first version is not NOTPAID"() {
        // Spec: plan.md §3.1 - ORDER_STATUS_TRANSITION, prev == null requires NOTPAID
        given:
        def order = orderWith()
        order.setStatus(OrderStatus.PAID)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_STATUS_TRANSITION
    }

    def "order: ORDER_STATUS_TRANSITION violation - #previousStatus to #status is not an edge"() {
        // Spec: plan.md §3.1 - ORDER_STATUS_TRANSITION, permitted edges
        given:
        def previous = orderAt(previousStatus)
        def order = nextVersionOf(previous)
        order.setStatus(status)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_STATUS_TRANSITION

        where:
        previousStatus         | status
        OrderStatus.NOTPAID    | OrderStatus.COLLECTED
        OrderStatus.NOTPAID    | OrderStatus.USED
        OrderStatus.PAID       | OrderStatus.USED
        OrderStatus.COLLECTED  | OrderStatus.PAID
        OrderStatus.USED       | OrderStatus.COLLECTED
    }

    def "order: ORDER_STATUS_TRANSITION permits #previousStatus to #status"() {
        // Spec: plan.md §3.1 - ORDER_STATUS_TRANSITION, permitted edges
        given:
        def order = nextVersionOf(orderAt(previousStatus))
        order.setStatus(status)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.status == status

        where:
        previousStatus         | status
        OrderStatus.NOTPAID    | OrderStatus.PAID
        OrderStatus.PAID       | OrderStatus.COLLECTED
        OrderStatus.COLLECTED  | OrderStatus.USED
        OrderStatus.USED       | OrderStatus.USED
    }

    def "order: ORDER_PRICE_POSITIVE violation - the price is negative"() {
        // Spec: plan.md §3.1 - ORDER_PRICE_POSITIVE
        given:
        def order = orderWith(ORDER_PRICE_NEGATIVE)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_PRICE_POSITIVE
    }

    def "order: ORDER_PRICE_POSITIVE on-point - the price is the smallest positive amount"() {
        // Spec: plan.md §3.1 - ORDER_PRICE_POSITIVE, last satisfying value
        given:
        def order = orderWith(ORDER_PRICE_ON_POINT)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.price == ORDER_PRICE_ON_POINT
    }

    def "order: ORDER_PRICE_POSITIVE off-point - the price is zero"() {
        // Spec: plan.md §3.1 - ORDER_PRICE_POSITIVE, first violating value
        given:
        def order = orderWith(ORDER_PRICE_OFF_POINT)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_PRICE_POSITIVE
    }

    def "order: ORDER_SEAT_NUMBER_POSITIVE on-point - the seat number is one"() {
        // Spec: plan.md §3.1 - ORDER_SEAT_NUMBER_POSITIVE, last satisfying value
        given:
        def order = orderWith(ORDER_PRICE, ORDER_SEAT_NUMBER_ON_POINT)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.seatNumber == ORDER_SEAT_NUMBER_ON_POINT
    }

    def "order: ORDER_SEAT_NUMBER_POSITIVE off-point - the seat number is zero"() {
        // Spec: plan.md §3.1 - ORDER_SEAT_NUMBER_POSITIVE, first violating value
        given:
        def order = orderWith(ORDER_PRICE, ORDER_SEAT_NUMBER_OFF_POINT)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_SEAT_NUMBER_POSITIVE
    }

    def "order: ORDER_DEPARTURE_AFTER_PURCHASE on-point - the order is bought at the departure instant"() {
        // Spec: plan.md §3.1 - ORDER_DEPARTURE_AFTER_PURCHASE, last satisfying value
        given:
        def order = orderWith(ORDER_PRICE, ORDER_SEAT_NUMBER, ORDER_BOUGHT_DATE_ON_POINT)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.boughtDate == ORDER_BOUGHT_DATE_ON_POINT
    }

    def "order: ORDER_DEPARTURE_AFTER_PURCHASE off-point - the order is bought one instant after departure"() {
        // Spec: plan.md §3.1 - ORDER_DEPARTURE_AFTER_PURCHASE, first violating value
        given:
        def order = orderWith(ORDER_PRICE, ORDER_SEAT_NUMBER, ORDER_BOUGHT_DATE_OFF_POINT)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_DEPARTURE_AFTER_PURCHASE
    }

    def "order: ORDER_CANCELLATION_FIELDS_SET violation - a cancelled order has no cancellation fields"() {
        // Spec: plan.md §3.1 - ORDER_CANCELLATION_FIELDS_SET
        given:
        def order = nextVersionOf(orderWith())
        order.setStatus(OrderStatus.CANCELLED)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_CANCELLATION_FIELDS_SET
    }

    def "order: ORDER_CANCELLATION_FIELDS_SET violation - a live order carries a cancellation time"() {
        // Spec: plan.md §3.1 - ORDER_CANCELLATION_FIELDS_SET, the biconditional in the other direction
        given:
        def order = nextVersionOf(orderWith())
        order.setCancelledTime(ORDER_CANCELLED_TIME)
        order.setRefundAmount(ORDER_REFUND_NONE)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_CANCELLATION_FIELDS_SET
    }

    def "order: ORDER_REFUND_AMOUNT - cancelling an unpaid order refunds nothing"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, the NOTPAID branch
        given:
        def order = cancellationOf(orderAt(OrderStatus.NOTPAID), ORDER_REFUND_NONE)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.refundAmount == ORDER_REFUND_NONE
    }

    def "order: ORDER_REFUND_AMOUNT violation - cancelling an unpaid order refunds the paid rate"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, the NOTPAID branch
        given:
        def order = cancellationOf(orderAt(OrderStatus.NOTPAID), ORDER_REFUND_PAID)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_REFUND_AMOUNT
    }

    def "order: ORDER_REFUND_AMOUNT - cancelling a paid order before departure refunds eighty percent"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, the paid branch
        given:
        def order = cancellationOf(orderAt(OrderStatus.PAID), ORDER_REFUND_PAID)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.refundAmount == ORDER_REFUND_PAID
    }

    def "order: ORDER_REFUND_AMOUNT violation - cancelling a paid order before departure refunds nothing"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, the paid branch
        given:
        def order = cancellationOf(orderAt(OrderStatus.PAID), ORDER_REFUND_NONE)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_REFUND_AMOUNT
    }

    def "order: ORDER_REFUND_AMOUNT on-point - cancelling exactly at departure still refunds eighty percent"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, last instant on the refundable side of departure
        given:
        def order = cancellationOf(orderAt(OrderStatus.PAID), ORDER_REFUND_PAID, ORDER_CANCELLED_TIME_ON_POINT)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.cancelledTime == ORDER_CANCELLED_TIME_ON_POINT
    }

    def "order: ORDER_REFUND_AMOUNT off-point - cancelling one instant after departure refunds nothing"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, first instant past departure
        given:
        def order = cancellationOf(orderAt(OrderStatus.PAID), ORDER_REFUND_NONE, ORDER_CANCELLED_TIME_OFF_POINT)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.refundAmount == ORDER_REFUND_NONE
    }

    def "order: ORDER_REFUND_AMOUNT violation - cancelling one instant after departure refunds eighty percent"() {
        // Spec: plan.md §3.1 - ORDER_REFUND_AMOUNT, first instant past departure
        given:
        def order = cancellationOf(orderAt(OrderStatus.PAID), ORDER_REFUND_PAID, ORDER_CANCELLED_TIME_OFF_POINT)

        when:
        order.verifyInvariants()

        then:
        def ex = thrown(TrainticketException)
        ex.message == ORDER_REFUND_AMOUNT
    }

    def "order: ORDER_REFUND_AMOUNT is not re-checked on a later commit of an already-cancelled order"() {
        // Spec: plan.md §3.1 - the prev.status != CANCELLED conjunct, which keeps DeleteOrder from
        // re-picking the paid branch for an order cancelled while NOTPAID.
        given:
        def cancelled = cancellationOf(orderAt(OrderStatus.NOTPAID), ORDER_REFUND_NONE)
        def order = nextVersionOf(cancelled)

        when:
        order.verifyInvariants()

        then:
        notThrown(TrainticketException)
        order.status == OrderStatus.CANCELLED
        order.refundAmount == ORDER_REFUND_NONE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
