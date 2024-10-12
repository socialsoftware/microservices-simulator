package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;

public class GetStudentsFunctionalityTCC extends WorkflowFunctionality {
    private List<UserDto> students;
    private final UserService userService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetStudentsFunctionalityTCC(UserService userService, CausalUnitOfWorkService unitOfWorkService,  
                            CausalUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.students = userService.getStudents(unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public List<UserDto> getStudents() {
        return students;
    }

    public void setStudents(List<UserDto> students) {
        this.students = students;
    }
}