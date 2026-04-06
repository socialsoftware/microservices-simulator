package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.ProductDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.ProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;


@Service
@Transactional(noRollbackFor = AdvancedException.class)
public class ProductService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductFactory productFactory;

    public ProductService() {}

    public ProductDto createProduct(CreateProductRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ProductDto productDto = new ProductDto();
            productDto.setName(createRequest.getName());
            productDto.setPrice(createRequest.getPrice());
            productDto.setAvailable(createRequest.getAvailable());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Product product = productFactory.createProduct(aggregateId, productDto);
            unitOfWorkService.registerChanged(product, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error creating product: " + e.getMessage());
        }
    }

    public ProductDto getProductById(Integer id, UnitOfWork unitOfWork) {
        try {
            Product product = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving product: " + e.getMessage());
        }
    }

    public List<ProductDto> getAllProducts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = productRepository.findAll().stream()
                .map(Product::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(productFactory::createProductDto)
                .collect(Collectors.toList());
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving product: " + e.getMessage());
        }
    }

    public ProductDto updateProduct(ProductDto productDto, UnitOfWork unitOfWork) {
        try {
            Integer id = productDto.getAggregateId();
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);
            if (productDto.getName() != null) {
                newProduct.setName(productDto.getName());
            }
            if (productDto.getPrice() != null) {
                newProduct.setPrice(productDto.getPrice());
            }
            newProduct.setAvailable(productDto.getAvailable());

            unitOfWorkService.registerChanged(newProduct, unitOfWork);            ProductUpdatedEvent event = new ProductUpdatedEvent(newProduct.getAggregateId(), newProduct.getName(), newProduct.getPrice(), newProduct.getAvailable());
            event.setPublisherAggregateVersion(newProduct.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return productFactory.createProductDto(newProduct);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error updating product: " + e.getMessage());
        }
    }

    public void deleteProduct(Integer id, UnitOfWork unitOfWork) {
        try {
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);
            newProduct.remove();
            unitOfWorkService.registerChanged(newProduct, unitOfWork);            unitOfWorkService.registerEvent(new ProductDeletedEvent(newProduct.getAggregateId()), unitOfWork);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error deleting product: " + e.getMessage());
        }
    }








}