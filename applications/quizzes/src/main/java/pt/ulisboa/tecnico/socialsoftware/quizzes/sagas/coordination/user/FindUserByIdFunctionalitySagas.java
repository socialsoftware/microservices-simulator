package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class FindUserByIdFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public FindUserByIdFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep findUserStep = new SagaSyncStep("findUserStep", () -> {
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
            this.setUserDto(userDto);
        });
    
        workflow.addStep(findUserStep);
    }
    

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }
}