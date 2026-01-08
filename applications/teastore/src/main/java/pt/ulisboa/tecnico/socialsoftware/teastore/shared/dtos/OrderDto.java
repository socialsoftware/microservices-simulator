package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.Order;

public class OrderDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer userAggregateId;
    private String time;
    private Double totalPriceInCents;
    private String addressName;
    private String address1;
    private String address2;
    private String creditCardCompany;
    private String creditCardNumber;
    private String creditCardExpiryDate;

    public OrderDto() {
    }

    public OrderDto(Order order) {
        this.aggregateId = order.getAggregateId();
        this.version = order.getVersion();
        this.state = order.getState();
        this.userAggregateId = order.getUser() != null ? order.getUser().getUserAggregateId() : null;
        this.time = order.getTime();
        this.totalPriceInCents = order.getTotalPriceInCents();
        this.addressName = order.getAddressName();
        this.address1 = order.getAddress1();
        this.address2 = order.getAddress2();
        this.creditCardCompany = order.getCreditCardCompany();
        this.creditCardNumber = order.getCreditCardNumber();
        this.creditCardExpiryDate = order.getCreditCardExpiryDate();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
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
}