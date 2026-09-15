package com.example.dummyapp.item.aggregate;

/** Runtime counterpart of the source-only dummyapp DTO used by verifier setup tests. */
public class ItemDto {
    private Integer aggregateId;
    private String name;
    private int price;
    private Integer orderId;
    private ItemDto quiz;

    public ItemDto() {}

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public ItemDto getQuiz() { return quiz; }
    public void setQuiz(ItemDto quiz) { this.quiz = quiz; }
}
