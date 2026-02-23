package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostAuthorDto;

@Entity
public class PostAuthor {
    @Id
    @GeneratedValue
    private Long id;
    private String authorName;
    private Integer authorAggregateId;
    private Integer authorVersion;
    private AggregateState authorState;
    @OneToOne
    private Post post;

    public PostAuthor() {

    }

    public PostAuthor(AuthorDto authorDto) {
        setAuthorAggregateId(authorDto.getAggregateId());
        setAuthorVersion(authorDto.getVersion());
        setAuthorState(authorDto.getState());
    }

    public PostAuthor(PostAuthorDto postAuthorDto) {
        setAuthorName(postAuthorDto.getName());
        setAuthorAggregateId(postAuthorDto.getAggregateId());
        setAuthorVersion(postAuthorDto.getVersion());
        setAuthorState(postAuthorDto.getState());
    }

    public PostAuthor(PostAuthor other) {
        setAuthorName(other.getAuthorName());
        setAuthorAggregateId(other.getAuthorAggregateId());
        setAuthorVersion(other.getAuthorVersion());
        setAuthorState(other.getAuthorState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getAuthorAggregateId() {
        return authorAggregateId;
    }

    public void setAuthorAggregateId(Integer authorAggregateId) {
        this.authorAggregateId = authorAggregateId;
    }

    public Integer getAuthorVersion() {
        return authorVersion;
    }

    public void setAuthorVersion(Integer authorVersion) {
        this.authorVersion = authorVersion;
    }

    public AggregateState getAuthorState() {
        return authorState;
    }

    public void setAuthorState(AggregateState authorState) {
        this.authorState = authorState;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }




    public PostAuthorDto buildDto() {
        PostAuthorDto dto = new PostAuthorDto();
        dto.setName(getAuthorName());
        dto.setAggregateId(getAuthorAggregateId());
        dto.setVersion(getAuthorVersion());
        dto.setState(getAuthorState());
        return dto;
    }
}