package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import java.time.LocalDateTime;

public class CreateLoanRequestDto {
    @NotNull
    private MemberDto member;
    @NotNull
    private BookDto book;
    @NotNull
    private LocalDateTime loanDate;
    @NotNull
    private LocalDateTime dueDate;

    public CreateLoanRequestDto() {}

    public CreateLoanRequestDto(MemberDto member, BookDto book, LocalDateTime loanDate, LocalDateTime dueDate) {
        this.member = member;
        this.book = book;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
    }

    public MemberDto getMember() {
        return member;
    }

    public void setMember(MemberDto member) {
        this.member = member;
    }
    public BookDto getBook() {
        return book;
    }

    public void setBook(BookDto book) {
        this.book = book;
    }
    public LocalDateTime getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDateTime loanDate) {
        this.loanDate = loanDate;
    }
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
}
