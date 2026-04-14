package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.subscribe.InvoiceSubscribesOrderCancelled;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.subscribe.InvoiceSubscribesPaymentAuthorized;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceOrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceUserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.InvoiceStatus;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Invoice extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "invoice")
    private InvoiceOrder order;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "invoice")
    private InvoiceUser user;
    private String invoiceNumber;
    private Double amountInCents;
    private String issuedAt;
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    public Invoice() {

    }

    public Invoice(Integer aggregateId, InvoiceDto invoiceDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setInvoiceNumber(invoiceDto.getInvoiceNumber());
        setAmountInCents(invoiceDto.getAmountInCents());
        setIssuedAt(invoiceDto.getIssuedAt());
        setStatus(InvoiceStatus.valueOf(invoiceDto.getStatus()));
        setOrder(invoiceDto.getOrder() != null ? new InvoiceOrder(invoiceDto.getOrder()) : null);
        setUser(invoiceDto.getUser() != null ? new InvoiceUser(invoiceDto.getUser()) : null);
    }


    public Invoice(Invoice other) {
        super(other);
        setOrder(other.getOrder() != null ? new InvoiceOrder(other.getOrder()) : null);
        setUser(other.getUser() != null ? new InvoiceUser(other.getUser()) : null);
        setInvoiceNumber(other.getInvoiceNumber());
        setAmountInCents(other.getAmountInCents());
        setIssuedAt(other.getIssuedAt());
        setStatus(other.getStatus());
    }

    public InvoiceOrder getOrder() {
        return order;
    }

    public void setOrder(InvoiceOrder order) {
        this.order = order;
        if (this.order != null) {
            this.order.setInvoice(this);
        }
    }

    public InvoiceUser getUser() {
        return user;
    }

    public void setUser(InvoiceUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setInvoice(this);
        }
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            eventSubscriptions.add(new InvoiceSubscribesPaymentAuthorized());
            eventSubscriptions.add(new InvoiceSubscribesOrderCancelled());
        }
        return eventSubscriptions;
    }



    private boolean invariantRule0() {
        return amountInCents > 0.0;
    }

    private boolean invariantRule1() {
        return this.invoiceNumber != null && this.invoiceNumber.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Invoice amount must be positive");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Invoice number cannot be empty");
        }
    }

    public InvoiceDto buildDto() {
        InvoiceDto dto = new InvoiceDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setOrder(getOrder() != null ? new InvoiceOrderDto(getOrder()) : null);
        dto.setUser(getUser() != null ? new InvoiceUserDto(getUser()) : null);
        dto.setInvoiceNumber(getInvoiceNumber());
        dto.setAmountInCents(getAmountInCents());
        dto.setIssuedAt(getIssuedAt());
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        return dto;
    }
}