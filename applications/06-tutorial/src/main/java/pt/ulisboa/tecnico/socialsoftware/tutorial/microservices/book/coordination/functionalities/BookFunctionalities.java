package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.webapi.requestDtos.CreateBookRequestDto;
import java.util.List;

@Service
public class BookFunctionalities {
    @Autowired
    private BookService bookService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public BookDto createBook(CreateBookRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateBookFunctionalitySagas createBookFunctionalitySagas = new CreateBookFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createBookFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createBookFunctionalitySagas.getCreatedBookDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public BookDto getBookById(Integer bookAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetBookByIdFunctionalitySagas getBookByIdFunctionalitySagas = new GetBookByIdFunctionalitySagas(
                        sagaUnitOfWorkService, bookAggregateId, sagaUnitOfWork, commandGateway);
                getBookByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getBookByIdFunctionalitySagas.getBookDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public BookDto updateBook(BookDto bookDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(bookDto);
                UpdateBookFunctionalitySagas updateBookFunctionalitySagas = new UpdateBookFunctionalitySagas(
                        sagaUnitOfWorkService, bookDto, sagaUnitOfWork, commandGateway);
                updateBookFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateBookFunctionalitySagas.getUpdatedBookDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteBook(Integer bookAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteBookFunctionalitySagas deleteBookFunctionalitySagas = new DeleteBookFunctionalitySagas(
                        sagaUnitOfWorkService, bookAggregateId, sagaUnitOfWork, commandGateway);
                deleteBookFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<BookDto> getAllBooks() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllBooksFunctionalitySagas getAllBooksFunctionalitySagas = new GetAllBooksFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllBooksFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllBooksFunctionalitySagas.getBooks();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(BookDto bookDto) {
        if (bookDto.getTitle() == null) {
            throw new TutorialException(BOOK_MISSING_TITLE);
        }
        if (bookDto.getAuthor() == null) {
            throw new TutorialException(BOOK_MISSING_AUTHOR);
        }
        if (bookDto.getGenre() == null) {
            throw new TutorialException(BOOK_MISSING_GENRE);
        }
}

    private void checkInput(CreateBookRequestDto createRequest) {
        if (createRequest.getTitle() == null) {
            throw new TutorialException(BOOK_MISSING_TITLE);
        }
        if (createRequest.getAuthor() == null) {
            throw new TutorialException(BOOK_MISSING_AUTHOR);
        }
        if (createRequest.getGenre() == null) {
            throw new TutorialException(BOOK_MISSING_GENRE);
        }
}
}