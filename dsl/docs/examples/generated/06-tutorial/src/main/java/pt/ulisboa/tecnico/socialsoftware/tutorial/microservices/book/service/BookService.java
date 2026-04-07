package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.BookDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.BookUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialException;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.webapi.requestDtos.CreateBookRequestDto;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanRepository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;


@Service
@Transactional(noRollbackFor = TutorialException.class)
public class BookService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookFactory bookFactory;

    @Autowired
    private BookServiceExtension extension;

    @Autowired
    private ApplicationContext applicationContext;

    public BookService() {}

    public BookDto createBook(CreateBookRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            BookDto bookDto = new BookDto();
            bookDto.setTitle(createRequest.getTitle());
            bookDto.setAuthor(createRequest.getAuthor());
            bookDto.setGenre(createRequest.getGenre());
            bookDto.setAvailable(createRequest.getAvailable());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Book book = bookFactory.createBook(aggregateId, bookDto);
            unitOfWorkService.registerChanged(book, unitOfWork);
            return bookFactory.createBookDto(book);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error creating book: " + e.getMessage());
        }
    }

    public BookDto getBookById(Integer id, UnitOfWork unitOfWork) {
        try {
            Book book = (Book) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return bookFactory.createBookDto(book);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error retrieving book: " + e.getMessage());
        }
    }

    public List<BookDto> getAllBooks(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = bookRepository.findAll().stream()
                .map(Book::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Book) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(bookFactory::createBookDto)
                .collect(Collectors.toList());
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error retrieving book: " + e.getMessage());
        }
    }

    public BookDto updateBook(BookDto bookDto, UnitOfWork unitOfWork) {
        try {
            Integer id = bookDto.getAggregateId();
            Book oldBook = (Book) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Book newBook = bookFactory.createBookFromExisting(oldBook);
            if (bookDto.getTitle() != null) {
                newBook.setTitle(bookDto.getTitle());
            }
            if (bookDto.getAuthor() != null) {
                newBook.setAuthor(bookDto.getAuthor());
            }
            if (bookDto.getGenre() != null) {
                newBook.setGenre(bookDto.getGenre());
            }
            newBook.setAvailable(bookDto.getAvailable());

            unitOfWorkService.registerChanged(newBook, unitOfWork);            BookUpdatedEvent event = new BookUpdatedEvent(newBook.getAggregateId(), newBook.getTitle(), newBook.getAuthor(), newBook.getGenre(), newBook.getAvailable());
            event.setPublisherAggregateVersion(newBook.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return bookFactory.createBookDto(newBook);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error updating book: " + e.getMessage());
        }
    }

    public void deleteBook(Integer id, UnitOfWork unitOfWork) {
        try {
            LoanRepository loanRepositoryRef = applicationContext.getBean(LoanRepository.class);
            boolean hasLoanReferences = loanRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != Book.AggregateState.DELETED)
                .anyMatch(s -> s.getBook() != null && id.equals(s.getBook().getBookAggregateId()));
            if (hasLoanReferences) {
                throw new TutorialException("Cannot delete book that has active loans");
            }
            Book oldBook = (Book) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Book newBook = bookFactory.createBookFromExisting(oldBook);
            newBook.remove();
            unitOfWorkService.registerChanged(newBook, unitOfWork);            unitOfWorkService.registerEvent(new BookDeletedEvent(newBook.getAggregateId()), unitOfWork);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error deleting book: " + e.getMessage());
        }
    }








}