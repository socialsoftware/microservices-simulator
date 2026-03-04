package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanBook;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanMember;

public class LoanValidationAnnotations {

    public static class MemberValidation {
        @NotNull
        private LoanMember member;
        
        public LoanMember getMember() {
            return member;
        }
        
        public void setMember(LoanMember member) {
            this.member = member;
        }
    }

    public static class BookValidation {
        @NotNull
        private LoanBook book;
        
        public LoanBook getBook() {
            return book;
        }
        
        public void setBook(LoanBook book) {
            this.book = book;
        }
    }

    public static class LoanDateValidation {
        @NotNull
        private LocalDateTime loanDate;
        
        public LocalDateTime getLoanDate() {
            return loanDate;
        }
        
        public void setLoanDate(LocalDateTime loanDate) {
            this.loanDate = loanDate;
        }
    }

    public static class DueDateValidation {
        @NotNull
        private LocalDateTime dueDate;
        
        public LocalDateTime getDueDate() {
            return dueDate;
        }
        
        public void setDueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
        }
    }

}