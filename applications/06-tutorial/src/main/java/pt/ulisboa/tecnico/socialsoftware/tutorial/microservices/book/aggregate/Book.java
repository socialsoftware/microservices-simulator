package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Book extends Aggregate {
    private String title;
    private String author;
    private String genre;
    private Boolean available;

    public Book() {

    }

    public Book(Integer aggregateId, BookDto bookDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(bookDto.getTitle());
        setAuthor(bookDto.getAuthor());
        setGenre(bookDto.getGenre());
        setAvailable(bookDto.getAvailable());
    }


    public Book(Book other) {
        super(other);
        setTitle(other.getTitle());
        setAuthor(other.getAuthor());
        setGenre(other.getGenre());
        setAvailable(other.getAvailable());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.title != null && this.title.length() > 0;
    }

    private boolean invariantRule1() {
        return this.author != null && this.author.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Book title cannot be blank");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Author name cannot be blank");
        }
    }

    public BookDto buildDto() {
        BookDto dto = new BookDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setTitle(getTitle());
        dto.setAuthor(getAuthor());
        dto.setGenre(getGenre());
        dto.setAvailable(getAvailable());
        return dto;
    }
}