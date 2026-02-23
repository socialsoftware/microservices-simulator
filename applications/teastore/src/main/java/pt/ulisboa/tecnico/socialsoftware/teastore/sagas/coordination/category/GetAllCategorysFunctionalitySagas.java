package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.category;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllCategorysFunctionalitySagas extends WorkflowFunctionality {
    private List<CategoryDto> categorys;
    private final CategoryService categoryService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllCategorysFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CategoryService categoryService) {
        this.categoryService = categoryService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllCategorysStep = new SagaSyncStep("getAllCategorysStep", () -> {
            List<CategoryDto> categorys = categoryService.getAllCategorys(unitOfWork);
            setCategorys(categorys);
        });

        workflow.addStep(getAllCategorysStep);
    }
    public List<CategoryDto> getCategorys() {
        return categorys;
    }

    public void setCategorys(List<CategoryDto> categorys) {
        this.categorys = categorys;
    }
}
