package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.functionalities.LoanFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi.requestDtos.CreateLoanRequestDto;

@RestController
public class LoanController {
    @Autowired
    private LoanFunctionalities loanFunctionalities;

    @PostMapping("/loans/create")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanDto createLoan(@RequestBody CreateLoanRequestDto createRequest) {
        return loanFunctionalities.createLoan(createRequest);
    }

    @GetMapping("/loans/{loanAggregateId}")
    public LoanDto getLoanById(@PathVariable Integer loanAggregateId) {
        return loanFunctionalities.getLoanById(loanAggregateId);
    }

    @PutMapping("/loans")
    public LoanDto updateLoan(@RequestBody LoanDto loanDto) {
        return loanFunctionalities.updateLoan(loanDto);
    }

    @DeleteMapping("/loans/{loanAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoan(@PathVariable Integer loanAggregateId) {
        loanFunctionalities.deleteLoan(loanAggregateId);
    }

    @GetMapping("/loans")
    public List<LoanDto> getAllLoans() {
        return loanFunctionalities.getAllLoans();
    }
}
