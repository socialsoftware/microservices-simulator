package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.trainticket.enums.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private Integer tripAggregateId;
    private Integer contactsAggregateId;
    private Integer userAggregateId;
    private LocalDateTime boughtDate;
    private LocalDate travelDate;
    private LocalDateTime departureTime;
    private String tripNumber;
    private String fromStationName;
    private String toStationName;
    private SeatClass seatClass;
    private Integer seatNumber;
    private String contactsName;
    private DocumentType contactsDocumentType;
    private String contactsDocumentNumber;
    private BigDecimal price;
    private OrderStatus status;
    private BigDecimal refundAmount;
    private LocalDateTime cancelledTime;

    public OrderDto() {
    }

    public OrderDto(Integer tripAggregateId, Integer contactsAggregateId, Integer userAggregateId,
                    LocalDateTime boughtDate, LocalDate travelDate, LocalDateTime departureTime,
                    String tripNumber, String fromStationName, String toStationName,
                    SeatClass seatClass, Integer seatNumber, String contactsName,
                    DocumentType contactsDocumentType, String contactsDocumentNumber, BigDecimal price) {
        this.tripAggregateId = tripAggregateId;
        this.contactsAggregateId = contactsAggregateId;
        this.userAggregateId = userAggregateId;
        this.boughtDate = boughtDate;
        this.travelDate = travelDate;
        this.departureTime = departureTime;
        this.tripNumber = tripNumber;
        this.fromStationName = fromStationName;
        this.toStationName = toStationName;
        this.seatClass = seatClass;
        this.seatNumber = seatNumber;
        this.contactsName = contactsName;
        this.contactsDocumentType = contactsDocumentType;
        this.contactsDocumentNumber = contactsDocumentNumber;
        this.price = price;
    }

    public OrderDto(Order order) {
        this.aggregateId = order.getAggregateId();
        this.version = order.getVersion();
        this.state = order.getState();
        this.tripAggregateId = order.getTripAggregateId();
        this.contactsAggregateId = order.getContactsAggregateId();
        this.userAggregateId = order.getUserAggregateId();
        this.boughtDate = order.getBoughtDate();
        this.travelDate = order.getTravelDate();
        this.departureTime = order.getDepartureTime();
        this.tripNumber = order.getTripNumber();
        this.fromStationName = order.getFromStationName();
        this.toStationName = order.getToStationName();
        this.seatClass = order.getSeatClass();
        this.seatNumber = order.getSeatNumber();
        this.contactsName = order.getContactsName();
        this.contactsDocumentType = order.getContactsDocumentType();
        this.contactsDocumentNumber = order.getContactsDocumentNumber();
        this.price = order.getPrice();
        this.status = order.getStatus();
        this.refundAmount = order.getRefundAmount();
        this.cancelledTime = order.getCancelledTime();
    }

    public boolean isActive() {
        return this.state == Aggregate.AggregateState.ACTIVE;
    }

    public Integer getAggregateId() {
        return this.aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Aggregate.AggregateState getState() {
        return this.state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public Integer getTripAggregateId() {
        return this.tripAggregateId;
    }

    public void setTripAggregateId(Integer tripAggregateId) {
        this.tripAggregateId = tripAggregateId;
    }

    public Integer getContactsAggregateId() {
        return this.contactsAggregateId;
    }

    public void setContactsAggregateId(Integer contactsAggregateId) {
        this.contactsAggregateId = contactsAggregateId;
    }

    public Integer getUserAggregateId() {
        return this.userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public LocalDateTime getBoughtDate() {
        return this.boughtDate;
    }

    public void setBoughtDate(LocalDateTime boughtDate) {
        this.boughtDate = boughtDate;
    }

    public LocalDate getTravelDate() {
        return this.travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public LocalDateTime getDepartureTime() {
        return this.departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public String getTripNumber() {
        return this.tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public String getFromStationName() {
        return this.fromStationName;
    }

    public void setFromStationName(String fromStationName) {
        this.fromStationName = fromStationName;
    }

    public String getToStationName() {
        return this.toStationName;
    }

    public void setToStationName(String toStationName) {
        this.toStationName = toStationName;
    }

    public SeatClass getSeatClass() {
        return this.seatClass;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public Integer getSeatNumber() {
        return this.seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getContactsName() {
        return this.contactsName;
    }

    public void setContactsName(String contactsName) {
        this.contactsName = contactsName;
    }

    public DocumentType getContactsDocumentType() {
        return this.contactsDocumentType;
    }

    public void setContactsDocumentType(DocumentType contactsDocumentType) {
        this.contactsDocumentType = contactsDocumentType;
    }

    public String getContactsDocumentNumber() {
        return this.contactsDocumentNumber;
    }

    public void setContactsDocumentNumber(String contactsDocumentNumber) {
        this.contactsDocumentNumber = contactsDocumentNumber;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
