package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;

public class FindUserByIdFunctionalityTCC extends WorkflowFunctionality {
    private UserDto userDto;
    private final UserService userService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public FindUserByIdFunctionalityTCC(UserService userService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
            this.setUserDto(userDto);
        });
    
        workflow.addStep(step);
    }
    

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }
}