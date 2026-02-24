package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;

/**
 * Invariant validation methods for Loan
 */
public class LoanInvariants {

    /**
     * Member cannot be null
     */
    public static void invariantMemberNotNull(Loan entity) {
        if (entity.getMember() == null) {
            throw new IllegalStateException("Member cannot be null");
        }
    }

    /**
     * Book cannot be null
     */
    public static void invariantBookNotNull(Loan entity) {
        if (entity.getBook() == null) {
            throw new IllegalStateException("Book cannot be null");
        }
    }

    /**
     * LoanDate cannot be null
     */
    public static void invariantLoanDateNotNull(Loan entity) {
        if (entity.getLoanDate() == null) {
            throw new IllegalStateException("LoanDate cannot be null");
        }
    }

    /**
     * DueDate cannot be null
     */
    public static void invariantDueDateNotNull(Loan entity) {
        if (entity.getDueDate() == null) {
            throw new IllegalStateException("DueDate cannot be null");
        }
    }

    /**
     * Loan aggregate must be in a valid state
     */
    public static void invariantLoanValid(Loan entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}