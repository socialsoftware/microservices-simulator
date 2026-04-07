package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
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
            productDto.setSku(createRequest.getSku());
            productDto.setName(createRequest.getName());
            productDto.setDescription(createRequest.getDescription());
            productDto.setPriceInCents(createRequest.getPriceInCents());
            productDto.setStock(createRequest.getStock());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Product product = productFactory.createProduct(aggregateId, productDto);
            unitOfWorkService.registerChanged(product, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating product: " + e.getMessage());
        }
    }

    public ProductDto getProductById(Integer id, UnitOfWork unitOfWork) {
        try {
            Product product = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return productFactory.createProductDto(product);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving product: " + e.getMessage());
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
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving product: " + e.getMessage());
        }
    }

    public ProductDto updateProduct(ProductDto productDto, UnitOfWork unitOfWork) {
        try {
            Integer id = productDto.getAggregateId();
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);
            if (productDto.getSku() != null) {
                newProduct.setSku(productDto.getSku());
            }
            if (productDto.getName() != null) {
                newProduct.setName(productDto.getName());
            }
            if (productDto.getDescription() != null) {
                newProduct.setDescription(productDto.getDescription());
            }
            if (productDto.getPriceInCents() != null) {
                newProduct.setPriceInCents(productDto.getPriceInCents());
            }
            if (productDto.getStock() != null) {
                newProduct.setStock(productDto.getStock());
            }

            unitOfWorkService.registerChanged(newProduct, unitOfWork);            ProductUpdatedEvent event = new ProductUpdatedEvent(newProduct.getAggregateId(), newProduct.getSku(), newProduct.getName(), newProduct.getPriceInCents(), newProduct.getStock());
            event.setPublisherAggregateVersion(newProduct.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return productFactory.createProductDto(newProduct);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating product: " + e.getMessage());
        }
    }

    public void deleteProduct(Integer id, UnitOfWork unitOfWork) {
        try {
            Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Product newProduct = productFactory.createProductFromExisting(oldProduct);
            newProduct.remove();
            unitOfWorkService.registerChanged(newProduct, unitOfWork);            unitOfWorkService.registerEvent(new ProductDeletedEvent(newProduct.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting product: " + e.getMessage());
        }
    }








}