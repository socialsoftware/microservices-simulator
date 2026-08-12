package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.category.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllCategorysFunctionalitySagas extends WorkflowFunctionality {
    private List<CategoryDto> categorys;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllCategorysFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllCategorysStep = new SagaStep("getAllCategorysStep", () -> {
            GetAllCategorysCommand cmd = new GetAllCategorysCommand(unitOfWork, ServiceMapping.CATEGORY.getServiceName());
            List<CategoryDto> categorys = (List<CategoryDto>) commandGateway.send(cmd);
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
