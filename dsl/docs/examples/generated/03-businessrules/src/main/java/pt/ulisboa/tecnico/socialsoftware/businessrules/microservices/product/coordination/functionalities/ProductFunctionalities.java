package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.exception.BusinessrulesErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.exception.BusinessrulesException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;
import java.util.List;

@Service
public class ProductFunctionalities {
    @Autowired
    private ProductService productService;

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
            throw new BusinessrulesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto createProduct(CreateProductRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateProductFunctionalitySagas createProductFunctionalitySagas = new CreateProductFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createProductFunctionalitySagas.getCreatedProductDto();
            default: throw new BusinessrulesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto getProductById(Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetProductByIdFunctionalitySagas getProductByIdFunctionalitySagas = new GetProductByIdFunctionalitySagas(
                        sagaUnitOfWorkService, productAggregateId, sagaUnitOfWork, commandGateway);
                getProductByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getProductByIdFunctionalitySagas.getProductDto();
            default: throw new BusinessrulesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto updateProduct(ProductDto productDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(productDto);
                UpdateProductFunctionalitySagas updateProductFunctionalitySagas = new UpdateProductFunctionalitySagas(
                        sagaUnitOfWorkService, productDto, sagaUnitOfWork, commandGateway);
                updateProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateProductFunctionalitySagas.getUpdatedProductDto();
            default: throw new BusinessrulesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteProduct(Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteProductFunctionalitySagas deleteProductFunctionalitySagas = new DeleteProductFunctionalitySagas(
                        sagaUnitOfWorkService, productAggregateId, sagaUnitOfWork, commandGateway);
                deleteProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new BusinessrulesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ProductDto> getAllProducts() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllProductsFunctionalitySagas getAllProductsFunctionalitySagas = new GetAllProductsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllProductsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllProductsFunctionalitySagas.getProducts();
            default: throw new BusinessrulesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(ProductDto productDto) {
        if (productDto.getName() == null) {
            throw new BusinessrulesException(PRODUCT_MISSING_NAME);
        }
        if (productDto.getSku() == null) {
            throw new BusinessrulesException(PRODUCT_MISSING_SKU);
        }
}

    private void checkInput(CreateProductRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new BusinessrulesException(PRODUCT_MISSING_NAME);
        }
        if (createRequest.getSku() == null) {
            throw new BusinessrulesException(PRODUCT_MISSING_SKU);
        }
}
}