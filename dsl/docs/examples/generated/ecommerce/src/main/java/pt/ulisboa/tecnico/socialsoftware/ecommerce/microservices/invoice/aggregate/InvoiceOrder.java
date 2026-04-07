package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceOrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;

@Entity
public class InvoiceOrder {
    @Id
    @GeneratedValue
    private Long id;
    private Double orderTotalInCents;
    private Integer orderAggregateId;
    private Integer orderVersion;
    private AggregateState orderState;
    @OneToOne
    private Invoice invoice;

    public InvoiceOrder() {

    }

    public InvoiceOrder(OrderDto orderDto) {
        setOrderAggregateId(orderDto.getAggregateId());
        setOrderVersion(orderDto.getVersion());
        setOrderState(orderDto.getState());
    }

    public InvoiceOrder(InvoiceOrderDto invoiceOrderDto) {
        setOrderTotalInCents(invoiceOrderDto.getTotalInCents());
        setOrderAggregateId(invoiceOrderDto.getAggregateId());
        setOrderVersion(invoiceOrderDto.getVersion());
        setOrderState(invoiceOrderDto.getState() != null ? AggregateState.valueOf(invoiceOrderDto.getState()) : null);
    }

    public InvoiceOrder(InvoiceOrder other) {
        setOrderTotalInCents(other.getOrderTotalInCents());
        setOrderAggregateId(other.getOrderAggregateId());
        setOrderVersion(other.getOrderVersion());
        setOrderState(other.getOrderState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getOrderTotalInCents() {
        return orderTotalInCents;
    }

    public void setOrderTotalInCents(Double orderTotalInCents) {
        this.orderTotalInCents = orderTotalInCents;
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public Integer getOrderVersion() {
        return orderVersion;
    }

    public void setOrderVersion(Integer orderVersion) {
        this.orderVersion = orderVersion;
    }

    public AggregateState getOrderState() {
        return orderState;
    }

    public void setOrderState(AggregateState orderState) {
        this.orderState = orderState;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }




    public InvoiceOrderDto buildDto() {
        InvoiceOrderDto dto = new InvoiceOrderDto();
        dto.setTotalInCents(getOrderTotalInCents());
        dto.setAggregateId(getOrderAggregateId());
        dto.setVersion(getOrderVersion());
        dto.setState(getOrderState() != null ? getOrderState().name() : null);
        return dto;
    }
}