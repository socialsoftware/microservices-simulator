package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddEnrollmentTeachersFunctionalitySagas extends WorkflowFunctionality {
    private List<EnrollmentTeacherDto> addedTeacherDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddEnrollmentTeachersFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer enrollmentId, List<EnrollmentTeacherDto> teacherDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentId, teacherDtos, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, List<EnrollmentTeacherDto> teacherDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addTeachersStep = new SagaStep("addTeachersStep", () -> {
            AddEnrollmentTeachersCommand cmd = new AddEnrollmentTeachersCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentId, teacherDtos);
            List<EnrollmentTeacherDto> addedTeacherDtos = (List<EnrollmentTeacherDto>) commandGateway.send(cmd);
            setAddedTeacherDtos(addedTeacherDtos);
        });

        workflow.addStep(addTeachersStep);
    }
    public List<EnrollmentTeacherDto> getAddedTeacherDtos() {
        return addedTeacherDtos;
    }

    public void setAddedTeacherDtos(List<EnrollmentTeacherDto> addedTeacherDtos) {
        this.addedTeacherDtos = addedTeacherDtos;
    }
}
