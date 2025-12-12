package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class UserSagaCoordination extends WorkflowFunctionality {
private UserDto userDto;
private SagaUserDto user;
private final UserService userService;
private final SagaUnitOfWorkService unitOfWorkService;

public UserSagaCoordination(UserService userService, SagaUnitOfWorkService
unitOfWorkService,
UserDto userDto, SagaUnitOfWork unitOfWork) {
this.userService = userService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(userDto, unitOfWork);
}

public void buildWorkflow(UserDto userDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public UserDto getUserDto() {
return userDto;
}

public void setUserDto(UserDto userDto) {
this.userDto = userDto;
}

public SagaUserDto getUser() {
return user;
}

public void setUser(SagaUserDto user) {
this.user = user;
}
}