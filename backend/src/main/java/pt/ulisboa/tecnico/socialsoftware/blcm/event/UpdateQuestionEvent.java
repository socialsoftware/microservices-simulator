package pt.ulisboa.tecnico.socialsoftware.blcm.event;

public class UpdateQuestionEvent extends DomainEvent{
    private Integer aggregateId;

    private String title;

    private String content;

    public UpdateQuestionEvent(Integer aggregateId, String title, String content) {
        this.aggregateId = aggregateId;
        this.title = title;
        this.content = content;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
