package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

import java.util.List;

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