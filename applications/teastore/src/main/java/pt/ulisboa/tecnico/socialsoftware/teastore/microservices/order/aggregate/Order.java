package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;

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

    public Order(Integer aggregateId, OrderUser user, OrderDto orderDto) {
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
        setUser(user);
    }

    public Order(Order other) {
        super(other);
        setUser(new OrderUser(other.getUser()));
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
    public void verifyInvariants() {
        // No invariants defined
    }

}