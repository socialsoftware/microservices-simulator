package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.GetStudentsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

import java.util.List;

public class GetStudentsFunctionalitySagas extends WorkflowFunctionality {
    private List<UserDto> students;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public GetStudentsFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,
            SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getStudentsStep = new SagaSyncStep("getStudentsStep", () -> {
            // List<UserDto> students = userService.getStudents(unitOfWork);
            GetStudentsCommand getStudentsCommand = new GetStudentsCommand(unitOfWork,
                    ServiceMapping.USER.getServiceName());
            List<UserDto> students = (List<UserDto>) CommandGateway.send(getStudentsCommand);
            this.setStudents(students);
        });

        workflow.addStep(getStudentsStep);
    }

    public List<UserDto> getStudents() {
        return students;
    }

    public void setStudents(List<UserDto> students) {
        this.students = students;
    }
}