package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import java.time.LocalDateTime;

public class CreatePostRequestDto {
    @NotNull
    private AuthorDto author;
    @NotNull
    private String title;
    @NotNull
    private String content;
    @NotNull
    private LocalDateTime publishedAt;

    public CreatePostRequestDto() {}

    public CreatePostRequestDto(AuthorDto author, String title, String content, LocalDateTime publishedAt) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
    }

    public AuthorDto getAuthor() {
        return author;
    }

    public void setAuthor(AuthorDto author) {
        this.author = author;
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
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
