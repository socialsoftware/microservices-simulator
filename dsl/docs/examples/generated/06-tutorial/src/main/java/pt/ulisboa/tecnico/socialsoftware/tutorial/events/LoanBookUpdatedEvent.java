package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class LoanBookUpdatedEvent extends Event {
    @Column(name = "loan_book_updated_event_book_aggregate_id")
    private Integer bookAggregateId;
    @Column(name = "loan_book_updated_event_book_version")
    private Integer bookVersion;
    @Column(name = "loan_book_updated_event_book_title")
    private String bookTitle;
    @Column(name = "loan_book_updated_event_book_author")
    private String bookAuthor;
    @Column(name = "loan_book_updated_event_book_genre")
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