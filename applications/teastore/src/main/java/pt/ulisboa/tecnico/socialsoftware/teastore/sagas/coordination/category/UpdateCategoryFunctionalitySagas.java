package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateCategoryFunctionalitySagas extends WorkflowFunctionality {
    private CategoryDto updatedCategoryDto;
    private final CategoryService categoryService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateCategoryFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CategoryService categoryService, CategoryDto categoryDto) {
        this.categoryService = categoryService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(categoryDto, unitOfWork);
    }

    public void buildWorkflow(CategoryDto categoryDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateCategoryStep = new SagaSyncStep("updateCategoryStep", () -> {
            CategoryDto updatedCategoryDto = categoryService.updateCategory(categoryDto, unitOfWork);
            setUpdatedCategoryDto(updatedCategoryDto);
        });

        workflow.addStep(updateCategoryStep);
    }
    public CategoryDto getUpdatedCategoryDto() {
        return updatedCategoryDto;
    }

    public void setUpdatedCategoryDto(CategoryDto updatedCategoryDto) {
        this.updatedCategoryDto = updatedCategoryDto;
    }
}
