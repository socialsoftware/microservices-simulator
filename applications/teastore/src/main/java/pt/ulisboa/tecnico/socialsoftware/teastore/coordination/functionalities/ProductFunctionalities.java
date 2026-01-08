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

    public ProductDto createProduct(ProductDto productDto) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateProductFunctionalitySagas createProductFunctionalitySagas = new CreateProductFunctionalitySagas(
                        productService, sagaUnitOfWorkService, productDto, sagaUnitOfWork);
                createProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createProductFunctionalitySagas.getCreatedProduct();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ProductDto findByProductId(Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindByProductIdFunctionalitySagas findByProductIdFunctionalitySagas = new FindByProductIdFunctionalitySagas(
                        productService, sagaUnitOfWorkService, productAggregateId, sagaUnitOfWork);
                findByProductIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findByProductIdFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ProductDto> findByCategory(String categoryName) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindByCategoryFunctionalitySagas findByCategoryFunctionalitySagas = new FindByCategoryFunctionalitySagas(
                        productService, sagaUnitOfWorkService, categoryName, sagaUnitOfWork);
                findByCategoryFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findByCategoryFunctionalitySagas.getFindByCategory();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteProduct(Integer productAggregateId) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteProductFunctionalitySagas deleteProductFunctionalitySagas = new DeleteProductFunctionalitySagas(
                        productService, sagaUnitOfWorkService, productAggregateId, sagaUnitOfWork);
                deleteProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}