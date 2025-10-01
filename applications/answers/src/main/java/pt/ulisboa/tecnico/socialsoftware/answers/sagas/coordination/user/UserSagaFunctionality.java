package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;

@Component
public class UserSagaFunctionality extends WorkflowFunctionality {
private final UserService userService;
private final SagaUnitOfWorkService unitOfWorkService;

public UserSagaFunctionality(UserService userService, SagaUnitOfWorkService
unitOfWorkService) {
this.userService = userService;
this.unitOfWorkService = unitOfWorkService;
}


}