package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.user;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetTeachersFunctionalitySagas extends WorkflowFunctionality {
    private List<UserDto> teachers;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetTeachersFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,  
                            SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTeachersStep = new SagaSyncStep("getTeachersStep", () -> {
            List<UserDto> teachers = userService.getTeachers(unitOfWork);
            this.setTeachers(teachers);
        });
    
        workflow.addStep(getTeachersStep);
    }
    

    public List<UserDto> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<UserDto> teachers) {
        this.teachers = teachers;
    }
}