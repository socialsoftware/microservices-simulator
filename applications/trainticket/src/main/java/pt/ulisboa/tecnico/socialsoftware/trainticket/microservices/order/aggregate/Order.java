package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_CANCELLATION_FIELDS_SET;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_DEPARTURE_AFTER_PURCHASE;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_PRICE_POSITIVE;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_REFUND_AMOUNT;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_SEAT_NUMBER_POSITIVE;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ORDER_STATUS_TRANSITION;

@Entity
@Table(name = "orders")
public abstract class Order extends Aggregate {
    private static final BigDecimal REFUND_RATE = new BigDecimal("0.80");

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    // ORDER_CONTRACT_FIELDS_FINAL: every purchased term is frozen at booking, so it is enforced by
    // the compiler - final fields, no setters - and carries no verifyInvariants() predicate.
    @Column(name = "trip_aggregate_id")
    private final Integer tripAggregateId;
    @Column(name = "contacts_aggregate_id")
    private final Integer contactsAggregateId;
    @Column(name = "user_aggregate_id")
    private final Integer userAggregateId;
    @Column(name = "bought_date")
    private final LocalDateTime boughtDate;
    @Column(name = "travel_date")
    private final LocalDate travelDate;
    @Column(name = "departure_time")
    private final LocalDateTime departureTime;
    @Column(name = "trip_number")
    private final String tripNumber;
    @Column(name = "from_station_name")
    private final String fromStationName;
    @Column(name = "to_station_name")
    private final String toStationName;
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class")
    private final SeatClass seatClass;
    @Column(name = "seat_number")
    private final Integer seatNumber;
    @Column(name = "contacts_name")
    private final String contactsName;
    @Enumerated(EnumType.STRING)
    @Column(name = "contacts_document_type")
    private final DocumentType contactsDocumentType;
    @Column(name = "contacts_document_number")
    private final String contactsDocumentNumber;
    @Column(name = "price", precision = 19, scale = 4)
    private final BigDecimal price;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;
    @Column(name = "refund_amount", precision = 19, scale = 4)
    private BigDecimal refundAmount;
    @Column(name = "cancelled_time")
    private LocalDateTime cancelledTime;

    public Order() {
        this.tripAggregateId = null;
        this.contactsAggregateId = null;
        this.userAggregateId = null;
        this.boughtDate = null;
        this.travelDate = null;
        this.departureTime = null;
        this.tripNumber = null;
        this.fromStationName = null;
        this.toStationName = null;
        this.seatClass = null;
        this.seatNumber = null;
        this.contactsName = null;
        this.contactsDocumentType = null;
        this.contactsDocumentNumber = null;
        this.price = null;
    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        this.tripAggregateId = orderDto.getTripAggregateId();
        this.contactsAggregateId = orderDto.getContactsAggregateId();
        this.userAggregateId = orderDto.getUserAggregateId();
        this.boughtDate = orderDto.getBoughtDate();
        this.travelDate = orderDto.getTravelDate();
        this.departureTime = orderDto.getDepartureTime();
        this.tripNumber = orderDto.getTripNumber();
        this.fromStationName = orderDto.getFromStationName();
        this.toStationName = orderDto.getToStationName();
        this.seatClass = orderDto.getSeatClass();
        this.seatNumber = orderDto.getSeatNumber();
        this.contactsName = orderDto.getContactsName();
        this.contactsDocumentType = orderDto.getContactsDocumentType();
        this.contactsDocumentNumber = orderDto.getContactsDocumentNumber();
        this.price = orderDto.getPrice();
        // A newly booked order is always NOTPAID (domain model §1), whatever the DTO carries.
        setStatus(OrderStatus.NOTPAID);
        setRefundAmount(null);
        setCancelledTime(null);
        setAggregateType(getClass().getSimpleName());
    }

    public Order(Order other) {
        super(other);
        this.tripAggregateId = other.getTripAggregateId();
        this.contactsAggregateId = other.getContactsAggregateId();
        this.userAggregateId = other.getUserAggregateId();
        this.boughtDate = other.getBoughtDate();
        this.travelDate = other.getTravelDate();
        this.departureTime = other.getDepartureTime();
        this.tripNumber = other.getTripNumber();
        this.fromStationName = other.getFromStationName();
        this.toStationName = other.getToStationName();
        this.seatClass = other.getSeatClass();
        this.seatNumber = other.getSeatNumber();
        this.contactsName = other.getContactsName();
        this.contactsDocumentType = other.getContactsDocumentType();
        this.contactsDocumentNumber = other.getContactsDocumentNumber();
        this.price = other.getPrice();
        setStatus(other.getStatus());
        setRefundAmount(other.getRefundAmount());
        setCancelledTime(other.getCancelledTime());
    }

    // Applied to the copy that supersedes this version, so this.status is still the pre-cancellation
    // status - the same value verifyRefundAmount() reads as prev.status once the copy is chained.
    public void cancel(LocalDateTime cancelledTime) {
        setRefundAmount(refundDueOn(cancelledTime));
        setCancelledTime(cancelledTime);
        setStatus(OrderStatus.CANCELLED);
    }

    private BigDecimal refundDueOn(LocalDateTime cancelledTime) {
        if (this.status == OrderStatus.NOTPAID || cancelledTime.isAfter(this.departureTime)) {
            return BigDecimal.ZERO;
        }
        return this.price.multiply(REFUND_RATE);
    }

    private static Map<OrderStatus, Set<OrderStatus>> allowedTransitions() {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.NOTPAID, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PAID, EnumSet.of(OrderStatus.COLLECTED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.COLLECTED, EnumSet.of(OrderStatus.USED));
        transitions.put(OrderStatus.USED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        return transitions;
    }

    @Override
    public void verifyInvariants() {
        verifyStatusTransition();
        verifyPricePositive();
        verifySeatNumberPositive();
        verifyDepartureAfterPurchase();
        verifyCancellationFieldsSet();
        verifyRefundAmount();
    }

    private void verifyStatusTransition() {
        Order previous = getPreviousOrder();
        if (previous == null) {
            if (this.status != OrderStatus.NOTPAID) {
                throw new TrainticketException(ORDER_STATUS_TRANSITION);
            }
            return;
        }
        OrderStatus previousStatus = previous.getStatus();
        if (this.status == previousStatus) {
            return;
        }
        if (this.status == null || !ALLOWED_TRANSITIONS.get(previousStatus).contains(this.status)) {
            throw new TrainticketException(ORDER_STATUS_TRANSITION);
        }
    }

    private void verifyPricePositive() {
        if (this.price == null || this.price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TrainticketException(ORDER_PRICE_POSITIVE);
        }
    }

    private void verifySeatNumberPositive() {
        if (this.seatNumber == null || this.seatNumber < 1) {
            throw new TrainticketException(ORDER_SEAT_NUMBER_POSITIVE);
        }
    }

    private void verifyDepartureAfterPurchase() {
        if (this.boughtDate == null || this.departureTime == null || this.boughtDate.isAfter(this.departureTime)) {
            throw new TrainticketException(ORDER_DEPARTURE_AFTER_PURCHASE);
        }
    }

    private void verifyCancellationFieldsSet() {
        boolean cancelled = this.status == OrderStatus.CANCELLED;
        boolean fieldsSet = this.cancelledTime != null && this.refundAmount != null;
        if (cancelled != fieldsSet) {
            throw new TrainticketException(ORDER_CANCELLATION_FIELDS_SET);
        }
    }

    private void verifyRefundAmount() {
        Order previous = getPreviousOrder();
        // The rule fires only on the commit that performs the cancellation: a later commit on an
        // already-cancelled order would re-pick the branch from the wrong previous status. A first
        // version can never be CANCELLED - verifyStatusTransition has already rejected that.
        if (this.status != OrderStatus.CANCELLED || previous == null
                || previous.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        BigDecimal expected;
        if (previous.getStatus() == OrderStatus.NOTPAID || this.cancelledTime.isAfter(this.departureTime)) {
            expected = BigDecimal.ZERO;
        } else {
            expected = this.price.multiply(REFUND_RATE);
        }
        if (this.refundAmount.compareTo(expected) != 0) {
            throw new TrainticketException(ORDER_REFUND_AMOUNT);
        }
    }

    private Order getPreviousOrder() {
        return getPrev() instanceof Order previous ? previous : null;
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public Integer getTripAggregateId() {
        return this.tripAggregateId;
    }

    public Integer getContactsAggregateId() {
        return this.contactsAggregateId;
    }

    public Integer getUserAggregateId() {
        return this.userAggregateId;
    }

    public LocalDateTime getBoughtDate() {
        return this.boughtDate;
    }

    public LocalDate getTravelDate() {
        return this.travelDate;
    }

    public LocalDateTime getDepartureTime() {
        return this.departureTime;
    }

    public String getTripNumber() {
        return this.tripNumber;
    }

    public String getFromStationName() {
        return this.fromStationName;
    }

    public String getToStationName() {
        return this.toStationName;
    }

    public SeatClass getSeatClass() {
        return this.seatClass;
    }

    public Integer getSeatNumber() {
        return this.seatNumber;
    }

    public String getContactsName() {
        return this.contactsName;
    }

    public DocumentType getContactsDocumentType() {
        return this.contactsDocumentType;
    }

    public String getContactsDocumentNumber() {
        return this.contactsDocumentNumber;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getRefundAmount() {
        return this.refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getCancelledTime() {
        return this.cancelledTime;
    }

    public void setCancelledTime(LocalDateTime cancelledTime) {
        this.cancelledTime = cancelledTime;
    }
}
