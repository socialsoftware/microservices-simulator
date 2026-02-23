package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.product.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateProductRequestDto;
import java.util.List;

@Service
public class ProductFunctionalities {
    @Autowired
    private ProductService productService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto createProduct(CreateProductRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateProductFunctionalitySagas createProductFunctionalitySagas = new CreateProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, productService, createRequest);
                createProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createProductFunctionalitySagas.getCreatedProductDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto getProductById(Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetProductByIdFunctionalitySagas getProductByIdFunctionalitySagas = new GetProductByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, productService, productAggregateId);
                getProductByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getProductByIdFunctionalitySagas.getProductDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto updateProduct(ProductDto productDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(productDto);
                UpdateProductFunctionalitySagas updateProductFunctionalitySagas = new UpdateProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, productService, productDto);
                updateProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateProductFunctionalitySagas.getUpdatedProductDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteProduct(Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteProductFunctionalitySagas deleteProductFunctionalitySagas = new DeleteProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, productService, productAggregateId);
                deleteProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ProductDto> getAllProducts() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllProductsFunctionalitySagas getAllProductsFunctionalitySagas = new GetAllProductsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, productService);
                getAllProductsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllProductsFunctionalitySagas.getProducts();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(ProductDto productDto) {
        if (productDto.getName() == null) {
            throw new TeastoreException(PRODUCT_MISSING_NAME);
        }
        if (productDto.getDescription() == null) {
            throw new TeastoreException(PRODUCT_MISSING_DESCRIPTION);
        }
}

    private void checkInput(CreateProductRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new TeastoreException(PRODUCT_MISSING_NAME);
        }
        if (createRequest.getDescription() == null) {
            throw new TeastoreException(PRODUCT_MISSING_DESCRIPTION);
        }
}
}