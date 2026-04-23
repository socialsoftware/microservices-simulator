package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class BookDeletedEvent extends Event {
    private Integer bookId;
    private String title;

    public BookDeletedEvent() {
        super();
    }

    public BookDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public BookDeletedEvent(Integer aggregateId, Integer bookId, String title) {
        super(aggregateId);
        setBookId(bookId);
        setTitle(title);
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}