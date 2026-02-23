package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateCategoryRequestDto;

public class CreateCategoryFunctionalitySagas extends WorkflowFunctionality {
    private CategoryDto createdCategoryDto;
    private final CategoryService categoryService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateCategoryFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CategoryService categoryService, CreateCategoryRequestDto createRequest) {
        this.categoryService = categoryService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCategoryRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createCategoryStep = new SagaSyncStep("createCategoryStep", () -> {
            CategoryDto createdCategoryDto = categoryService.createCategory(createRequest, unitOfWork);
            setCreatedCategoryDto(createdCategoryDto);
        });

        workflow.addStep(createCategoryStep);
    }
    public CategoryDto getCreatedCategoryDto() {
        return createdCategoryDto;
    }

    public void setCreatedCategoryDto(CategoryDto createdCategoryDto) {
        this.createdCategoryDto = createdCategoryDto;
    }
}
