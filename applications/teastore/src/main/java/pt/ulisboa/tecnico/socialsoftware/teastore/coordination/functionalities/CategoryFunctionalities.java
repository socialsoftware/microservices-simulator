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
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateCategoryRequestDto;
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

    public CategoryDto createCategory(CreateCategoryRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateCategoryFunctionalitySagas createCategoryFunctionalitySagas = new CreateCategoryFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, categoryService, createRequest);
                createCategoryFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCategoryFunctionalitySagas.getCreatedCategoryDto();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CategoryDto getCategoryById(Integer categoryAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCategoryByIdFunctionalitySagas getCategoryByIdFunctionalitySagas = new GetCategoryByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, categoryService, categoryAggregateId);
                getCategoryByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCategoryByIdFunctionalitySagas.getCategoryDto();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CategoryDto updateCategory(CategoryDto categoryDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(categoryDto);
                UpdateCategoryFunctionalitySagas updateCategoryFunctionalitySagas = new UpdateCategoryFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, categoryService, categoryDto);
                updateCategoryFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateCategoryFunctionalitySagas.getUpdatedCategoryDto();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteCategory(Integer categoryAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteCategoryFunctionalitySagas deleteCategoryFunctionalitySagas = new DeleteCategoryFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, categoryService, categoryAggregateId);
                deleteCategoryFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CategoryDto> getAllCategorys() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllCategorysFunctionalitySagas getAllCategorysFunctionalitySagas = new GetAllCategorysFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, categoryService);
                getAllCategorysFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllCategorysFunctionalitySagas.getCategorys();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(CategoryDto categoryDto) {
        if (categoryDto.getName() == null) {
            throw new TeastoreException(CATEGORY_MISSING_NAME);
        }
        if (categoryDto.getDescription() == null) {
            throw new TeastoreException(CATEGORY_MISSING_DESCRIPTION);
        }
}

    private void checkInput(CreateCategoryRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new TeastoreException(CATEGORY_MISSING_NAME);
        }
        if (createRequest.getDescription() == null) {
            throw new TeastoreException(CATEGORY_MISSING_DESCRIPTION);
        }
}
}