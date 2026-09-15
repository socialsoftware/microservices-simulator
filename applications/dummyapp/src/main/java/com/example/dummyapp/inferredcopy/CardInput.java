package com.example.dummyapp.inferredcopy;

public class CardInput {
    private Integer aggregateId;
    private String caption;
    public CardInput(Integer id, String text) { aggregateId=id; caption=text; }
    public Integer getAggregateId() { return aggregateId; }
    public String getCaption() { return caption; }
    public void setCaption(String value) { caption=value; }
}
