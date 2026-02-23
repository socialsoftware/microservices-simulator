package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateBookRequestDto {
    @NotNull
    private String title;
    @NotNull
    private String author;
    @NotNull
    private String genre;
    @NotNull
    private Boolean available;

    public CreateBookRequestDto() {}

    public CreateBookRequestDto(String title, String author, String genre, Boolean available) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.available = available;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
