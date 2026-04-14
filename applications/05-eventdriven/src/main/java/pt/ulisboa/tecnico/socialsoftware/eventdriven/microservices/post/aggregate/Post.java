package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate;

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

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe.PostSubscribesAuthorDeleted;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe.PostSubscribesAuthorDeletedAuthorExists;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe.PostSubscribesAuthorUpdated;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostAuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Post extends Aggregate {
    private String title;
    private String content;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "post")
    private PostAuthor author;
    private LocalDateTime publishedAt;

    public Post() {

    }

    public Post(Integer aggregateId, PostDto postDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(postDto.getTitle());
        setContent(postDto.getContent());
        setPublishedAt(postDto.getPublishedAt());
        setAuthor(postDto.getAuthor() != null ? new PostAuthor(postDto.getAuthor()) : null);
    }


    public Post(Post other) {
        super(other);
        setTitle(other.getTitle());
        setContent(other.getContent());
        setAuthor(other.getAuthor() != null ? new PostAuthor(other.getAuthor()) : null);
        setPublishedAt(other.getPublishedAt());
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

    public PostAuthor getAuthor() {
        return author;
    }

    public void setAuthor(PostAuthor author) {
        this.author = author;
        if (this.author != null) {
            this.author.setPost(this);
        }
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantAuthorExists(eventSubscriptions);
            eventSubscriptions.add(new PostSubscribesAuthorUpdated());
            eventSubscriptions.add(new PostSubscribesAuthorDeleted(this));
        }
        return eventSubscriptions;
    }
    private void interInvariantAuthorExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new PostSubscribesAuthorDeletedAuthorExists(this.getAuthor()));
    }


    private boolean invariantRule0() {
        return this.title != null && this.title.length() > 0;
    }

    private boolean invariantRule1() {
        return this.author != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Post title cannot be blank");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Post must have an author");
        }
    }

    public PostDto buildDto() {
        PostDto dto = new PostDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setTitle(getTitle());
        dto.setContent(getContent());
        dto.setAuthor(getAuthor() != null ? new PostAuthorDto(getAuthor()) : null);
        dto.setPublishedAt(getPublishedAt());
        return dto;
    }
}