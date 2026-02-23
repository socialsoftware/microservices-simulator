package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetCategoryByIdFunctionalitySagas extends WorkflowFunctionality {
    private CategoryDto categoryDto;
    private final CategoryService categoryService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetCategoryByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CategoryService categoryService, Integer categoryAggregateId) {
        this.categoryService = categoryService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(categoryAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer categoryAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCategoryStep = new SagaSyncStep("getCategoryStep", () -> {
            CategoryDto categoryDto = categoryService.getCategoryById(categoryAggregateId, unitOfWork);
            setCategoryDto(categoryDto);
        });

        workflow.addStep(getCategoryStep);
    }
    public CategoryDto getCategoryDto() {
        return categoryDto;
    }

    public void setCategoryDto(CategoryDto categoryDto) {
        this.categoryDto = categoryDto;
    }
}
