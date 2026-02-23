package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanMemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanBookDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish.LoanDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish.LoanUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish.LoanMemberDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish.LoanMemberUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish.LoanBookDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish.LoanBookUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialException;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos.CreateLoanRequestDto;


@Service
@Transactional
public class LoanService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanFactory loanFactory;

    public LoanService() {}

    public LoanDto createLoan(CreateLoanRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            LoanDto loanDto = new LoanDto();
            loanDto.setLoanDate(createRequest.getLoanDate());
            loanDto.setDueDate(createRequest.getDueDate());
            if (createRequest.getMember() != null) {
                LoanMemberDto memberDto = new LoanMemberDto();
                memberDto.setAggregateId(createRequest.getMember().getAggregateId());
                memberDto.setVersion(createRequest.getMember().getVersion());
                memberDto.setState(createRequest.getMember().getState());
                loanDto.setMember(memberDto);
            }
            if (createRequest.getBook() != null) {
                LoanBookDto bookDto = new LoanBookDto();
                bookDto.setAggregateId(createRequest.getBook().getAggregateId());
                bookDto.setVersion(createRequest.getBook().getVersion());
                bookDto.setState(createRequest.getBook().getState());
                loanDto.setBook(bookDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Loan loan = loanFactory.createLoan(aggregateId, loanDto);
            unitOfWorkService.registerChanged(loan, unitOfWork);
            return loanFactory.createLoanDto(loan);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error creating loan: " + e.getMessage());
        }
    }

    public LoanDto getLoanById(Integer id, UnitOfWork unitOfWork) {
        try {
            Loan loan = (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return loanFactory.createLoanDto(loan);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error retrieving loan: " + e.getMessage());
        }
    }

    public List<LoanDto> getAllLoans(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = loanRepository.findAll().stream()
                .map(Loan::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(loanFactory::createLoanDto)
                .collect(Collectors.toList());
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error retrieving loan: " + e.getMessage());
        }
    }

    public LoanDto updateLoan(LoanDto loanDto, UnitOfWork unitOfWork) {
        try {
            Integer id = loanDto.getAggregateId();
            Loan oldLoan = (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Loan newLoan = loanFactory.createLoanFromExisting(oldLoan);
            if (loanDto.getLoanDate() != null) {
                newLoan.setLoanDate(loanDto.getLoanDate());
            }
            if (loanDto.getDueDate() != null) {
                newLoan.setDueDate(loanDto.getDueDate());
            }

            unitOfWorkService.registerChanged(newLoan, unitOfWork);            LoanUpdatedEvent event = new LoanUpdatedEvent(newLoan.getAggregateId(), newLoan.getLoanDate(), newLoan.getDueDate());
            event.setPublisherAggregateVersion(newLoan.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return loanFactory.createLoanDto(newLoan);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error updating loan: " + e.getMessage());
        }
    }

    public void deleteLoan(Integer id, UnitOfWork unitOfWork) {
        try {
            Loan oldLoan = (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Loan newLoan = loanFactory.createLoanFromExisting(oldLoan);
            newLoan.remove();
            unitOfWorkService.registerChanged(newLoan, unitOfWork);            unitOfWorkService.registerEvent(new LoanDeletedEvent(newLoan.getAggregateId()), unitOfWork);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error deleting loan: " + e.getMessage());
        }
    }








}