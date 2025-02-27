package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetStudentsFunctionalitySagas extends WorkflowFunctionality {
    private List<UserDto> students;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetStudentsFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,  
                            SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getStudentsStep = new SagaSyncStep("getStudentsStep", () -> {
            List<UserDto> students = userService.getStudents(unitOfWork);
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