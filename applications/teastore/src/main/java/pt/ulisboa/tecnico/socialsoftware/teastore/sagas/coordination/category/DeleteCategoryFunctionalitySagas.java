package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteCategoryFunctionalitySagas extends WorkflowFunctionality {
    private final CategoryService categoryService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteCategoryFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CategoryService categoryService, Integer categoryAggregateId) {
        this.categoryService = categoryService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(categoryAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer categoryAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteCategoryStep = new SagaSyncStep("deleteCategoryStep", () -> {
            categoryService.deleteCategory(categoryAggregateId, unitOfWork);
        });

        workflow.addStep(deleteCategoryStep);
    }
}
