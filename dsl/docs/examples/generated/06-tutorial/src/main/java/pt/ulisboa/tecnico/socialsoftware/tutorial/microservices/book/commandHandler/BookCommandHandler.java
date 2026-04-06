package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.book.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;

import java.util.logging.Logger;

@Component
public class BookCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(BookCommandHandler.class.getName());

    @Autowired
    private BookService bookService;

    @Override
    protected String getAggregateTypeName() {
        return "Book";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateBookCommand cmd -> handleCreateBook(cmd);
            case GetBookByIdCommand cmd -> handleGetBookById(cmd);
            case GetAllBooksCommand cmd -> handleGetAllBooks(cmd);
            case UpdateBookCommand cmd -> handleUpdateBook(cmd);
            case DeleteBookCommand cmd -> handleDeleteBook(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateBook(CreateBookCommand cmd) {
        logger.info("handleCreateBook");
        try {
            return bookService.createBook(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetBookById(GetBookByIdCommand cmd) {
        logger.info("handleGetBookById");
        try {
            return bookService.getBookById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllBooks(GetAllBooksCommand cmd) {
        logger.info("handleGetAllBooks");
        try {
            return bookService.getAllBooks(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateBook(UpdateBookCommand cmd) {
        logger.info("handleUpdateBook");
        try {
            return bookService.updateBook(cmd.getBookDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteBook(DeleteBookCommand cmd) {
        logger.info("handleDeleteBook");
        try {
            bookService.deleteBook(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
