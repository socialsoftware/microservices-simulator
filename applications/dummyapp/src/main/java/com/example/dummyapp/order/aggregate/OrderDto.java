package com.example.dummyapp.order.aggregate;

public class OrderDto {

    private Integer aggregateId;
    private String status;

    public OrderDto() {}

    public OrderDto(Order order) {
        this.aggregateId = order.getAggregateId();
        this.status = order.getStatus();
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
