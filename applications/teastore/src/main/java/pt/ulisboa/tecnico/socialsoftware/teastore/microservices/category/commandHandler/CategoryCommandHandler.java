package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.category.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;

import java.util.logging.Logger;

@Component
public class CategoryCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CategoryCommandHandler.class.getName());

    @Autowired
    private CategoryService categoryService;

    @Override
    protected String getAggregateTypeName() {
        return "Category";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateCategoryCommand cmd -> handleCreateCategory(cmd);
            case GetCategoryByIdCommand cmd -> handleGetCategoryById(cmd);
            case GetAllCategorysCommand cmd -> handleGetAllCategorys(cmd);
            case UpdateCategoryCommand cmd -> handleUpdateCategory(cmd);
            case DeleteCategoryCommand cmd -> handleDeleteCategory(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateCategory(CreateCategoryCommand cmd) {
        logger.info("handleCreateCategory");
        try {
            return categoryService.createCategory(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCategoryById(GetCategoryByIdCommand cmd) {
        logger.info("handleGetCategoryById");
        try {
            return categoryService.getCategoryById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllCategorys(GetAllCategorysCommand cmd) {
        logger.info("handleGetAllCategorys");
        try {
            return categoryService.getAllCategorys(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateCategory(UpdateCategoryCommand cmd) {
        logger.info("handleUpdateCategory");
        try {
            return categoryService.updateCategory(cmd.getCategoryDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteCategory(DeleteCategoryCommand cmd) {
        logger.info("handleDeleteCategory");
        try {
            categoryService.deleteCategory(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
