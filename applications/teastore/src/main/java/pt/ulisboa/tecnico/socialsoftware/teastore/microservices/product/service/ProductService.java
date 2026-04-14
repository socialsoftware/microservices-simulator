package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductCategoryDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.ProductDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.ProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.ProductCategoryUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;


@Service
@Transactional(noRollbackFor = TeastoreException.class)
public class ProductService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductFactory productFactory;

    @Autowired
    private ProductServiceExtension extension;

    public ProductService() {}

    public ProductDto createProduct(CreateProductRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ProductDto productDto = new ProductDto();
            productDto.setName(createRequest.getName());
            productDto.setDescription(createRequest.getDescription());
            productDto.setListPriceInCents(createRequest.getListPriceInCents());
            if (createRequest.getProductCategory() != null) {
                Category refSource = (Category) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getProductCategory().getAggregateId(), unitOfWork);
                CategoryDto refSourceDto = new CategoryDto(refSource);
                ProductCategoryDto productCategoryDto = new ProductCategoryDto();
                productCategoryDto.setAggregateId(refSourceDto.getAggregateId());
                productCategoryDto.setVersion(refSourceDto.getVersion());
                productCategoryDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                productCategoryDto.setName(refSourceDto.getName());
                productCategoryDto.setDescription(refSourceDto.getDescription());
                productDto.setProductCategory(productCategoryDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Product product = productFactory.createProduct(aggregateId, productDto);
            unitOfWorkService.registerChanged(product, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error creating product: " + e.getMessage());
        }
    }

    public ProductDto getProductById(Integer id, UnitOfWork unitOfWork) {
        try {
            Product product = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving product: " + e.getMessage());
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
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving product: " + e.getMessage());
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
            if (productDto.getDescription() != null) {
                newProduct.setDescription(productDto.getDescription());
            }
            if (productDto.getListPriceInCents() != null) {
                newProduct.setListPriceInCents(productDto.getListPriceInCents());
            }

            unitOfWorkService.registerChanged(newProduct, unitOfWork);            ProductUpdatedEvent event = new ProductUpdatedEvent(newProduct.getAggregateId(), newProduct.getName(), newProduct.getDescription(), newProduct.getListPriceInCents());
            event.setPublisherAggregateVersion(newProduct.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return productFactory.createProductDto(newProduct);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error updating product: " + e.getMessage());
        }
    }

    public void deleteProduct(Integer id, UnitOfWork unitOfWork) {
        try {
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);
            newProduct.remove();
            unitOfWorkService.registerChanged(newProduct, unitOfWork);            unitOfWorkService.registerEvent(new ProductDeletedEvent(newProduct.getAggregateId()), unitOfWork);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error deleting product: " + e.getMessage());
        }
    }




    public Product handleCategoryUpdatedEvent(Integer aggregateId, Integer categoryAggregateId, Integer categoryVersion, String categoryName, String categoryDescription, UnitOfWork unitOfWork) {
        try {
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);



            unitOfWorkService.registerChanged(newProduct, unitOfWork);

        unitOfWorkService.registerEvent(
            new ProductCategoryUpdatedEvent(
                    newProduct.getAggregateId(),
                    categoryAggregateId,
                    categoryVersion,
                    categoryName,
                    categoryDescription
            ),
            unitOfWork
        );

            return newProduct;
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error handling CategoryUpdatedEvent product: " + e.getMessage());
        }
    }




}