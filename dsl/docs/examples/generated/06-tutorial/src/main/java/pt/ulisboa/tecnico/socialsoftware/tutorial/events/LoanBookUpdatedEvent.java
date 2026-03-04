package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class LoanBookUpdatedEvent extends Event {
    private Integer bookAggregateId;
    private Integer bookVersion;
    private String bookTitle;
    private String bookAuthor;
    private String bookGenre;

    public LoanBookUpdatedEvent() {
        super();
    }

    public LoanBookUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public LoanBookUpdatedEvent(Integer aggregateId, Integer bookAggregateId, Integer bookVersion, String bookTitle, String bookAuthor, String bookGenre) {
        super(aggregateId);
        setBookAggregateId(bookAggregateId);
        setBookVersion(bookVersion);
        setBookTitle(bookTitle);
        setBookAuthor(bookAuthor);
        setBookGenre(bookGenre);
    }

    public Integer getBookAggregateId() {
        return bookAggregateId;
    }

    public void setBookAggregateId(Integer bookAggregateId) {
        this.bookAggregateId = bookAggregateId;
    }

    public Integer getBookVersion() {
        return bookVersion;
    }

    public void setBookVersion(Integer bookVersion) {
        this.bookVersion = bookVersion;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getBookGenre() {
        return bookGenre;
    }

    public void setBookGenre(String bookGenre) {
        this.bookGenre = bookGenre;
    }

}