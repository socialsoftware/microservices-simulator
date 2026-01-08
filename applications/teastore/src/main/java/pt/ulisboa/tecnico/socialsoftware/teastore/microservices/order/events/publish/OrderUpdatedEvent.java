package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderUpdatedEvent extends Event {
    private String time;
    private Double totalPriceInCents;
    private String addressName;
    private String address1;
    private String address2;
    private String creditCardCompany;
    private String creditCardNumber;
    private String creditCardExpiryDate;

    public OrderUpdatedEvent() {
    }

    public OrderUpdatedEvent(Integer aggregateId, String time, Double totalPriceInCents, String addressName, String address1, String address2, String creditCardCompany, String creditCardNumber, String creditCardExpiryDate) {
        super(aggregateId);
        setTime(time);
        setTotalPriceInCents(totalPriceInCents);
        setAddressName(addressName);
        setAddress1(address1);
        setAddress2(address2);
        setCreditCardCompany(creditCardCompany);
        setCreditCardNumber(creditCardNumber);
        setCreditCardExpiryDate(creditCardExpiryDate);
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