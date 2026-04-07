package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceUser;

public class InvoiceUserDto implements Serializable {
    private String username;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public InvoiceUserDto() {
    }

    public InvoiceUserDto(InvoiceUser invoiceUser) {
        this.username = invoiceUser.getUserName();
        this.email = invoiceUser.getUserEmail();
        this.aggregateId = invoiceUser.getUserAggregateId();
        this.version = invoiceUser.getUserVersion();
        this.state = invoiceUser.getUserState() != null ? invoiceUser.getUserState().name() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}