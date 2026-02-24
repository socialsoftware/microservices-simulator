package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;

import java.util.logging.Logger;

@Component
public class AuthorCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(AuthorCommandHandler.class.getName());

    @Autowired
    private AuthorService authorService;

    @Override
    protected String getAggregateTypeName() {
        return "Author";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateAuthorCommand cmd -> handleCreateAuthor(cmd);
            case GetAuthorByIdCommand cmd -> handleGetAuthorById(cmd);
            case GetAllAuthorsCommand cmd -> handleGetAllAuthors(cmd);
            case UpdateAuthorCommand cmd -> handleUpdateAuthor(cmd);
            case DeleteAuthorCommand cmd -> handleDeleteAuthor(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateAuthor(CreateAuthorCommand cmd) {
        logger.info("handleCreateAuthor");
        try {
            return authorService.createAuthor(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAuthorById(GetAuthorByIdCommand cmd) {
        logger.info("handleGetAuthorById");
        try {
            return authorService.getAuthorById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllAuthors(GetAllAuthorsCommand cmd) {
        logger.info("handleGetAllAuthors");
        try {
            return authorService.getAllAuthors(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateAuthor(UpdateAuthorCommand cmd) {
        logger.info("handleUpdateAuthor");
        try {
            return authorService.updateAuthor(cmd.getAuthorDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteAuthor(DeleteAuthorCommand cmd) {
        logger.info("handleDeleteAuthor");
        try {
            authorService.deleteAuthor(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
