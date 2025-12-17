package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;
import java.util.List;

public class SearchUsersFunctionalitySagas extends WorkflowFunctionality {
    private List<UserDto> usersSearched;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public SearchUsersFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService, String name, String username, UserRole role, Boolean active, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(name, username, role, active, unitOfWork);
    }

    public void buildWorkflow(String name, String username, UserRole role, Boolean active, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchUsersStep = new SagaSyncStep("searchUsersStep", () -> {
            List<UserDto> usersSearched = userService.searchUsers(name, username, role, active, unitOfWork);
            setUsersSearched(usersSearched);
        });

        workflow.addStep(searchUsersStep);
    }

    public List<UserDto> getUsersSearched() {
        return usersSearched;
    }

    public void setUsersSearched(List<UserDto> usersSearched) {
        this.usersSearched = usersSearched;
    }
}
