package pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanBook;

public class LoanBookDto implements Serializable {
    private String title;
    private String author;
    private String genre;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public LoanBookDto() {
    }

    public LoanBookDto(LoanBook loanBook) {
        this.title = loanBook.getBookTitle();
        this.author = loanBook.getBookAuthor();
        this.genre = loanBook.getBookGenre();
        this.aggregateId = loanBook.getBookAggregateId();
        this.version = loanBook.getBookVersion();
        this.state = loanBook.getBookState();
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
}