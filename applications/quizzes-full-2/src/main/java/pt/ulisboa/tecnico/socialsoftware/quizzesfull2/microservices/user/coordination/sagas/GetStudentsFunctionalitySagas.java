package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetStudentsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.util.List;

public class GetStudentsFunctionalitySagas extends WorkflowFunctionality {
    private List<UserDto> students;

    public GetStudentsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStudentsStep = new SagaStep("getStudentsStep", () -> {
            GetStudentsCommand cmd = new GetStudentsCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName());
            this.students = (List<UserDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getStudentsStep);
    }

    public List<UserDto> getStudents() {
        return students;
    }
}
