package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanBookDto;

@Entity
public class LoanBook {
    @Id
    @GeneratedValue
    private Long id;
    private String bookTitle;
    private String bookAuthor;
    private String bookGenre;
    private Integer bookAggregateId;
    private Integer bookVersion;
    private AggregateState bookState;
    @OneToOne
    private Loan loan;

    public LoanBook() {

    }

    public LoanBook(BookDto bookDto) {
        setBookAggregateId(bookDto.getAggregateId());
        setBookVersion(bookDto.getVersion());
        setBookState(bookDto.getState());
    }

    public LoanBook(LoanBookDto loanBookDto) {
        setBookTitle(loanBookDto.getTitle());
        setBookAuthor(loanBookDto.getAuthor());
        setBookGenre(loanBookDto.getGenre());
        setBookAggregateId(loanBookDto.getAggregateId());
        setBookVersion(loanBookDto.getVersion());
        setBookState(loanBookDto.getState());
    }

    public LoanBook(LoanBook other) {
        setBookTitle(other.getBookTitle());
        setBookAuthor(other.getBookAuthor());
        setBookGenre(other.getBookGenre());
        setBookAggregateId(other.getBookAggregateId());
        setBookVersion(other.getBookVersion());
        setBookState(other.getBookState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AggregateState getBookState() {
        return bookState;
    }

    public void setBookState(AggregateState bookState) {
        this.bookState = bookState;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }




    public LoanBookDto buildDto() {
        LoanBookDto dto = new LoanBookDto();
        dto.setTitle(getBookTitle());
        dto.setAuthor(getBookAuthor());
        dto.setGenre(getBookGenre());
        dto.setAggregateId(getBookAggregateId());
        dto.setVersion(getBookVersion());
        dto.setState(getBookState());
        return dto;
    }
}