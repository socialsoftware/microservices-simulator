package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderCustomerDto;

@Entity
public class OrderCustomer {
    @Id
    @GeneratedValue
    private Long id;
    private String customerName;
    private String customerEmail;
    private Integer customerAggregateId;
    private Integer customerVersion;
    private AggregateState customerState;
    @OneToOne
    private Order order;

    public OrderCustomer() {

    }

    public OrderCustomer(CustomerDto customerDto) {
        setCustomerAggregateId(customerDto.getAggregateId());
        setCustomerVersion(customerDto.getVersion());
        setCustomerState(customerDto.getState());
    }

    public OrderCustomer(OrderCustomerDto orderCustomerDto) {
        setCustomerName(orderCustomerDto.getName());
        setCustomerEmail(orderCustomerDto.getEmail());
        setCustomerAggregateId(orderCustomerDto.getAggregateId());
        setCustomerVersion(orderCustomerDto.getVersion());
        setCustomerState(orderCustomerDto.getState());
    }

    public OrderCustomer(OrderCustomer other) {
        setCustomerName(other.getCustomerName());
        setCustomerEmail(other.getCustomerEmail());
        setCustomerAggregateId(other.getCustomerAggregateId());
        setCustomerVersion(other.getCustomerVersion());
        setCustomerState(other.getCustomerState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public Integer getCustomerAggregateId() {
        return customerAggregateId;
    }

    public void setCustomerAggregateId(Integer customerAggregateId) {
        this.customerAggregateId = customerAggregateId;
    }

    public Integer getCustomerVersion() {
        return customerVersion;
    }

    public void setCustomerVersion(Integer customerVersion) {
        this.customerVersion = customerVersion;
    }

    public AggregateState getCustomerState() {
        return customerState;
    }

    public void setCustomerState(AggregateState customerState) {
        this.customerState = customerState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderCustomerDto buildDto() {
        OrderCustomerDto dto = new OrderCustomerDto();
        dto.setName(getCustomerName());
        dto.setEmail(getCustomerEmail());
        dto.setAggregateId(getCustomerAggregateId());
        dto.setVersion(getCustomerVersion());
        dto.setState(getCustomerState());
        return dto;
    }
}