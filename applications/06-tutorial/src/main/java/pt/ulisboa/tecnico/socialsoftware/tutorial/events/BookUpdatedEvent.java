package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class BookUpdatedEvent extends Event {
    @Column(name = "book_updated_event_title")
    private String title;
    @Column(name = "book_updated_event_author")
    private String author;
    @Column(name = "book_updated_event_genre")
    private String genre;
    @Column(name = "book_updated_event_available")
    private Boolean available;

    public BookUpdatedEvent() {
        super();
    }

    public BookUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookUpdatedEvent(Integer aggregateId, String title, String author, String genre, Boolean available) {
        super(aggregateId);
        setTitle(title);
        setAuthor(author);
        setGenre(genre);
        setAvailable(available);
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