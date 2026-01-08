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
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.category.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import java.util.List;

@Service
public class CategoryFunctionalities {
    @Autowired
    private CategoryService categoryService;

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

    public CategoryDto createCategory(CategoryDto categoryDto) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateCategoryFunctionalitySagas createCategoryFunctionalitySagas = new CreateCategoryFunctionalitySagas(
                        categoryService, sagaUnitOfWorkService, categoryDto, sagaUnitOfWork);
                createCategoryFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCategoryFunctionalitySagas.getCreatedCategory();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CategoryDto> findAllCategories() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindAllCategoriesFunctionalitySagas findAllCategoriesFunctionalitySagas = new FindAllCategoriesFunctionalitySagas(
                        categoryService, sagaUnitOfWorkService, sagaUnitOfWork);
                findAllCategoriesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findAllCategoriesFunctionalitySagas.getFindAllCategories();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}