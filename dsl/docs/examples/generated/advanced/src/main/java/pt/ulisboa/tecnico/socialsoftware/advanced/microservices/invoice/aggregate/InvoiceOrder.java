package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceOrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;

@Entity
public class InvoiceOrder {
    @Id
    @GeneratedValue
    private Long id;
    private AggregateState orderState;
    private Set<Integer> orderProductIds = new HashSet<>();
    private Integer orderAggregateId;
    private Integer orderVersion;
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
        setOrderState(invoiceOrderDto.getState());
        setOrderProductIds(invoiceOrderDto.getOrderProductIds() != null ? invoiceOrderDto.getOrderProductIds().stream().map(Integer::new).collect(Collectors.toSet()) : null);
        setOrderAggregateId(invoiceOrderDto.getAggregateId());
        setOrderVersion(invoiceOrderDto.getVersion());
    }

    public InvoiceOrder(InvoiceOrder other) {
        setOrderState(other.getOrderState());
        setOrderProductIds(new HashSet<>(other.getOrderProductIds()));
        setOrderAggregateId(other.getOrderAggregateId());
        setOrderVersion(other.getOrderVersion());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AggregateState getOrderState() {
        return orderState;
    }

    public void setOrderState(AggregateState orderState) {
        this.orderState = orderState;
    }

    public Set<Integer> getOrderProductIds() {
        return orderProductIds;
    }

    public void setOrderProductIds(Set<Integer> orderProductIds) {
        this.orderProductIds = orderProductIds;
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

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }




    public InvoiceOrderDto buildDto() {
        InvoiceOrderDto dto = new InvoiceOrderDto();
        dto.setState(getOrderState());
        dto.setOrderProductIds(getOrderProductIds());
        dto.setAggregateId(getOrderAggregateId());
        dto.setVersion(getOrderVersion());
        return dto;
    }
}