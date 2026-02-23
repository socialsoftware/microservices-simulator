package pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;

public class PostDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String content;
    private PostAuthorDto author;
    private LocalDateTime publishedAt;

    public PostDto() {
    }

    public PostDto(Post post) {
        this.aggregateId = post.getAggregateId();
        this.version = post.getVersion();
        this.state = post.getState();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.author = post.getAuthor() != null ? new PostAuthorDto(post.getAuthor()) : null;
        this.publishedAt = post.getPublishedAt();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
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

    public PostAuthorDto getAuthor() {
        return author;
    }

    public void setAuthor(PostAuthorDto author) {
        this.author = author;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}