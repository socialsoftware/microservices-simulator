package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.businessrules.events.ProductDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.businessrules.events.ProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.exception.BusinessrulesException;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;


@Service
@Transactional
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
            productDto.setSku(createRequest.getSku());
            productDto.setPrice(createRequest.getPrice());
            productDto.setStockQuantity(createRequest.getStockQuantity());
            productDto.setActive(createRequest.getActive());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Product product = productFactory.createProduct(aggregateId, productDto);
            unitOfWorkService.registerChanged(product, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (BusinessrulesException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessrulesException("Error creating product: " + e.getMessage());
        }
    }

    public ProductDto getProductById(Integer id, UnitOfWork unitOfWork) {
        try {
            Product product = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (BusinessrulesException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessrulesException("Error retrieving product: " + e.getMessage());
        }
    }

    public List<ProductDto> getAllProducts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = productRepository.findAll().stream()
                .map(Product::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(productFactory::createProductDto)
                .collect(Collectors.toList());
        } catch (BusinessrulesException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessrulesException("Error retrieving product: " + e.getMessage());
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
            if (productDto.getSku() != null) {
                newProduct.setSku(productDto.getSku());
            }
            if (productDto.getPrice() != null) {
                newProduct.setPrice(productDto.getPrice());
            }
            if (productDto.getStockQuantity() != null) {
                newProduct.setStockQuantity(productDto.getStockQuantity());
            }
            newProduct.setActive(productDto.getActive());

            unitOfWorkService.registerChanged(newProduct, unitOfWork);            ProductUpdatedEvent event = new ProductUpdatedEvent(newProduct.getAggregateId(), newProduct.getName(), newProduct.getSku(), newProduct.getPrice(), newProduct.getStockQuantity(), newProduct.getActive());
            event.setPublisherAggregateVersion(newProduct.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return productFactory.createProductDto(newProduct);
        } catch (BusinessrulesException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessrulesException("Error updating product: " + e.getMessage());
        }
    }

    public void deleteProduct(Integer id, UnitOfWork unitOfWork) {
        try {
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);
            newProduct.remove();
            unitOfWorkService.registerChanged(newProduct, unitOfWork);            unitOfWorkService.registerEvent(new ProductDeletedEvent(newProduct.getAggregateId()), unitOfWork);
        } catch (BusinessrulesException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessrulesException("Error deleting product: " + e.getMessage());
        }
    }








}