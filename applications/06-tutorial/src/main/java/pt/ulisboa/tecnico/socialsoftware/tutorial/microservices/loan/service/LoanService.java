package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanMemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanBookDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.LoanDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.LoanUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialException;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi.requestDtos.CreateLoanRequestDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;


@Service
@Transactional(noRollbackFor = TutorialException.class)
public class LoanService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanFactory loanFactory;

    @Autowired
    private LoanServiceExtension extension;

    public LoanService() {}

    public LoanDto createLoan(CreateLoanRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            LoanDto loanDto = new LoanDto();
            loanDto.setLoanDate(createRequest.getLoanDate());
            loanDto.setDueDate(createRequest.getDueDate());
            if (createRequest.getMember() != null) {
                Member refSource = (Member) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getMember().getAggregateId(), unitOfWork);
                MemberDto refSourceDto = new MemberDto(refSource);
                LoanMemberDto memberDto = new LoanMemberDto();
                memberDto.setAggregateId(refSourceDto.getAggregateId());
                memberDto.setVersion(refSourceDto.getVersion());
                memberDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                memberDto.setName(refSourceDto.getName());
                memberDto.setEmail(refSourceDto.getEmail());
                loanDto.setMember(memberDto);
            }
            if (createRequest.getBook() != null) {
                Book refSource = (Book) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getBook().getAggregateId(), unitOfWork);
                BookDto refSourceDto = new BookDto(refSource);
                LoanBookDto bookDto = new LoanBookDto();
                bookDto.setAggregateId(refSourceDto.getAggregateId());
                bookDto.setVersion(refSourceDto.getVersion());
                bookDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                bookDto.setTitle(refSourceDto.getTitle());
                bookDto.setAuthor(refSourceDto.getAuthor());
                bookDto.setGenre(refSourceDto.getGenre());
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
                .map(id -> {
                    try {
                        return (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
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