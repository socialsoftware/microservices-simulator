package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.user;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

public class GetTeachersFunctionalityTCC extends WorkflowFunctionality {
    private List<UserDto> teachers;
    private final UserService userService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetTeachersFunctionalityTCC(UserService userService, CausalUnitOfWorkService unitOfWorkService,  
                            CausalUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.teachers = userService.getTeachers(unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public List<UserDto> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<UserDto> teachers) {
        this.teachers = teachers;
    }
}