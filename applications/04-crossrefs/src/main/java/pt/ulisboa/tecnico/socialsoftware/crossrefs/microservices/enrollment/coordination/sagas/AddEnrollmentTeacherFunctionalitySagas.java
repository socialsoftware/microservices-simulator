package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddEnrollmentTeacherFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentTeacherDto addedTeacherDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddEnrollmentTeacherFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentId, teacherAggregateId, teacherDto, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addTeacherStep = new SagaStep("addTeacherStep", () -> {
            AddEnrollmentTeacherCommand cmd = new AddEnrollmentTeacherCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentId, teacherAggregateId, teacherDto);
            EnrollmentTeacherDto addedTeacherDto = (EnrollmentTeacherDto) commandGateway.send(cmd);
            setAddedTeacherDto(addedTeacherDto);
        });

        workflow.addStep(addTeacherStep);
    }
    public EnrollmentTeacherDto getAddedTeacherDto() {
        return addedTeacherDto;
    }

    public void setAddedTeacherDto(EnrollmentTeacherDto addedTeacherDto) {
        this.addedTeacherDto = addedTeacherDto;
    }
}
