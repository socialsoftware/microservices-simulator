package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.subscribe.OrderSubscribesUserDeletedOrderUserExists;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.subscribe.OrderSubscribesUserUpdated;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderUserDto;

@Entity
public abstract class Order extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderUser user;
    private String time;
    private Double totalPriceInCents;
    private String addressName;
    private String address1;
    private String address2;
    private String creditCardCompany;
    private String creditCardNumber;
    private String creditCardExpiryDate;

    public Order() {

    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTime(orderDto.getTime());
        setTotalPriceInCents(orderDto.getTotalPriceInCents());
        setAddressName(orderDto.getAddressName());
        setAddress1(orderDto.getAddress1());
        setAddress2(orderDto.getAddress2());
        setCreditCardCompany(orderDto.getCreditCardCompany());
        setCreditCardNumber(orderDto.getCreditCardNumber());
        setCreditCardExpiryDate(orderDto.getCreditCardExpiryDate());
        setUser(orderDto.getUser() != null ? new OrderUser(orderDto.getUser()) : null);
    }


    public Order(Order other) {
        super(other);
        setUser(other.getUser() != null ? new OrderUser(other.getUser()) : null);
        setTime(other.getTime());
        setTotalPriceInCents(other.getTotalPriceInCents());
        setAddressName(other.getAddressName());
        setAddress1(other.getAddress1());
        setAddress2(other.getAddress2());
        setCreditCardCompany(other.getCreditCardCompany());
        setCreditCardNumber(other.getCreditCardNumber());
        setCreditCardExpiryDate(other.getCreditCardExpiryDate());
    }

    public OrderUser getUser() {
        return user;
    }

    public void setUser(OrderUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setOrder(this);
        }
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Double getTotalPriceInCents() {
        return totalPriceInCents;
    }

    public void setTotalPriceInCents(Double totalPriceInCents) {
        this.totalPriceInCents = totalPriceInCents;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCreditCardCompany() {
        return creditCardCompany;
    }

    public void setCreditCardCompany(String creditCardCompany) {
        this.creditCardCompany = creditCardCompany;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCreditCardExpiryDate() {
        return creditCardExpiryDate;
    }

    public void setCreditCardExpiryDate(String creditCardExpiryDate) {
        this.creditCardExpiryDate = creditCardExpiryDate;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantOrderUserExists(eventSubscriptions);
            eventSubscriptions.add(new OrderSubscribesUserUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantOrderUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesUserDeletedOrderUserExists(this.getUser()));
    }

    @Override
    public void verifyInvariants() {
    }

    public OrderDto buildDto() {
        OrderDto dto = new OrderDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUser(getUser() != null ? new OrderUserDto(getUser()) : null);
        dto.setTime(getTime());
        dto.setTotalPriceInCents(getTotalPriceInCents());
        dto.setAddressName(getAddressName());
        dto.setAddress1(getAddress1());
        dto.setAddress2(getAddress2());
        dto.setCreditCardCompany(getCreditCardCompany());
        dto.setCreditCardNumber(getCreditCardNumber());
        dto.setCreditCardExpiryDate(getCreditCardExpiryDate());
        return dto;
    }
}