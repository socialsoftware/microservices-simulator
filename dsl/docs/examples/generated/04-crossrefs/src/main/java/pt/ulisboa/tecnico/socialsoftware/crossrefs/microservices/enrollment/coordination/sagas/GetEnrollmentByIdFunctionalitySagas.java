package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetEnrollmentByIdFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentDto enrollmentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetEnrollmentByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer enrollmentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getEnrollmentStep = new SagaStep("getEnrollmentStep", () -> {
            GetEnrollmentByIdCommand cmd = new GetEnrollmentByIdCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentAggregateId);
            EnrollmentDto enrollmentDto = (EnrollmentDto) commandGateway.send(cmd);
            setEnrollmentDto(enrollmentDto);
        });

        workflow.addStep(getEnrollmentStep);
    }
    public EnrollmentDto getEnrollmentDto() {
        return enrollmentDto;
    }

    public void setEnrollmentDto(EnrollmentDto enrollmentDto) {
        this.enrollmentDto = enrollmentDto;
    }
}
