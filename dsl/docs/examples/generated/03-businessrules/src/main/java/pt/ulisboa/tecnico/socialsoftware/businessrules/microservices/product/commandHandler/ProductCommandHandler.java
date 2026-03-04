package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.businessrules.command.product.*;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.service.ProductService;

import java.util.logging.Logger;

@Component
public class ProductCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ProductCommandHandler.class.getName());

    @Autowired
    private ProductService productService;

    @Override
    protected String getAggregateTypeName() {
        return "Product";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateProductCommand cmd -> handleCreateProduct(cmd);
            case GetProductByIdCommand cmd -> handleGetProductById(cmd);
            case GetAllProductsCommand cmd -> handleGetAllProducts(cmd);
            case UpdateProductCommand cmd -> handleUpdateProduct(cmd);
            case DeleteProductCommand cmd -> handleDeleteProduct(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateProduct(CreateProductCommand cmd) {
        logger.info("handleCreateProduct");
        try {
            return productService.createProduct(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetProductById(GetProductByIdCommand cmd) {
        logger.info("handleGetProductById");
        try {
            return productService.getProductById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllProducts(GetAllProductsCommand cmd) {
        logger.info("handleGetAllProducts");
        try {
            return productService.getAllProducts(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateProduct(UpdateProductCommand cmd) {
        logger.info("handleUpdateProduct");
        try {
            return productService.updateProduct(cmd.getProductDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteProduct(DeleteProductCommand cmd) {
        logger.info("handleDeleteProduct");
        try {
            productService.deleteProduct(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
