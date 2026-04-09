package com.example.dummyapp.item.aggregate;

public class ItemDto {

    private Integer aggregateId;
    private String name;
    private int price;
    private Integer orderId;

    public ItemDto() {}

    public ItemDto(Item item) {
        this.aggregateId = item.getAggregateId();
        this.name = item.getName();
        this.price = item.getPrice();
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
}
