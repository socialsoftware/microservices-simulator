package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class BookUpdatedEvent extends Event {
    private String title;
    private String author;
    private String genre;
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