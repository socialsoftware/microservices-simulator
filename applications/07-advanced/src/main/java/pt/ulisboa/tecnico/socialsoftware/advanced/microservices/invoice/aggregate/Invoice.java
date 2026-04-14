package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate;

import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe.InvoiceSubscribesCustomerDeletedCustomerExists;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe.InvoiceSubscribesOrderDeleted;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.subscribe.InvoiceSubscribesOrderDeletedOrderExists;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceCustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceOrderDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Invoice extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "invoice")
    private InvoiceOrder order;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "invoice")
    private InvoiceCustomer customer;
    private Double totalAmount;
    private LocalDateTime issuedAt;
    private Boolean paid;

    public Invoice() {

    }

    public Invoice(Integer aggregateId, InvoiceDto invoiceDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTotalAmount(invoiceDto.getTotalAmount());
        setIssuedAt(invoiceDto.getIssuedAt());
        setPaid(invoiceDto.getPaid());
        setOrder(invoiceDto.getOrder() != null ? new InvoiceOrder(invoiceDto.getOrder()) : null);
        setCustomer(invoiceDto.getCustomer() != null ? new InvoiceCustomer(invoiceDto.getCustomer()) : null);
    }


    public Invoice(Invoice other) {
        super(other);
        setOrder(other.getOrder() != null ? new InvoiceOrder(other.getOrder()) : null);
        setCustomer(other.getCustomer() != null ? new InvoiceCustomer(other.getCustomer()) : null);
        setTotalAmount(other.getTotalAmount());
        setIssuedAt(other.getIssuedAt());
        setPaid(other.getPaid());
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

    public InvoiceCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(InvoiceCustomer customer) {
        this.customer = customer;
        if (this.customer != null) {
            this.customer.setInvoice(this);
        }
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantOrderExists(eventSubscriptions);
            interInvariantCustomerExists(eventSubscriptions);
            eventSubscriptions.add(new InvoiceSubscribesOrderDeleted(this));
        }
        return eventSubscriptions;
    }
    private void interInvariantOrderExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new InvoiceSubscribesOrderDeletedOrderExists(this.getOrder()));
    }

    private void interInvariantCustomerExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new InvoiceSubscribesCustomerDeletedCustomerExists(this.getCustomer()));
    }


    private boolean invariantRule0() {
        return this.order != null;
    }

    private boolean invariantRule1() {
        return this.customer != null;
    }

    private boolean invariantRule2() {
        return totalAmount > 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Invoice must reference an order");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Invoice must have a customer");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "Invoice total must be positive");
        }
    }

    public InvoiceDto buildDto() {
        InvoiceDto dto = new InvoiceDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setOrder(getOrder() != null ? new InvoiceOrderDto(getOrder()) : null);
        dto.setCustomer(getCustomer() != null ? new InvoiceCustomerDto(getCustomer()) : null);
        dto.setTotalAmount(getTotalAmount());
        dto.setIssuedAt(getIssuedAt());
        dto.setPaid(getPaid());
        return dto;
    }
}