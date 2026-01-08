package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.publish.ProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.publish.ProductDeletedEvent;

@Service
public class ProductService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final ProductRepository productRepository;

    @Autowired
    private ProductFactory productFactory;

    public ProductService(UnitOfWorkService unitOfWorkService, ProductRepository productRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.productRepository = productRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProductDto createProduct(ProductCategory productCategory, ProductDto productDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Product product = productFactory.createProduct(aggregateId, productCategory, productDto);
        unitOfWorkService.registerChanged(product, unitOfWork);
        return productFactory.createProductDto(product);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProductDto getProductById(Integer aggregateId, UnitOfWork unitOfWork) {
        return productFactory.createProductDto((Product) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProductDto updateProduct(ProductDto productDto, UnitOfWork unitOfWork) {
        Integer aggregateId = productDto.getAggregateId();
        Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Product newProduct = productFactory.createProductFromExisting(oldProduct);
        newProduct.setName(productDto.getName());
        newProduct.setDescription(productDto.getDescription());
        newProduct.setListPriceInCents(productDto.getListPriceInCents());
        unitOfWorkService.registerChanged(newProduct, unitOfWork);
        unitOfWorkService.registerEvent(new ProductUpdatedEvent(newProduct.getAggregateId(), newProduct.getName(), newProduct.getDescription(), newProduct.getListPriceInCents()), unitOfWork);
        return productFactory.createProductDto(newProduct);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteProduct(Integer aggregateId, UnitOfWork unitOfWork) {
        Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Product newProduct = productFactory.createProductFromExisting(oldProduct);
        newProduct.remove();
        unitOfWorkService.registerChanged(newProduct, unitOfWork);
        unitOfWorkService.registerEvent(new ProductDeletedEvent(newProduct.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ProductDto> searchProducts(String name, String description, Integer categoryAggregateId, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = productRepository.findAll().stream()
                .filter(entity -> {
                    if (name != null) {
                        if (!entity.getName().equals(name)) {
                            return false;
                        }
                    }
                    if (description != null) {
                        if (!entity.getDescription().equals(description)) {
                            return false;
                        }
                    }
                    if (categoryAggregateId != null) {
                        if (!entity.getProductCategory().getCategoryAggregateId().equals(categoryAggregateId)) {
                            return false;
                        }
                                            }
                    return true;
                })
                .map(Product::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(productFactory::createProductDto)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Product findByProductId(Integer productAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement findByProductId method
        return null;
    }

}
