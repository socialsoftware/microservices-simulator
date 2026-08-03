package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.ActivateUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.AnonymizeUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.CreateUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.DeleteUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.GetStudentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.GetTeachersFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.GetUserByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.UpdateUserNameFunctionalitySagas;

import java.util.List;

@Service
public class UserFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

    public UserDto getUserById(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getUserById");
        GetUserByIdFunctionalitySagas saga = new GetUserByIdFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getUserDto();
    }

    public List<UserDto> getStudents() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getStudents");
        GetStudentsFunctionalitySagas saga = new GetStudentsFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getStudents();
    }

    public List<UserDto> getTeachers() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTeachers");
        GetTeachersFunctionalitySagas saga = new GetTeachersFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTeachers();
    }

    public UserDto createUser(UserDto userDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createUser");
        CreateUserFunctionalitySagas saga = new CreateUserFunctionalitySagas(
                unitOfWorkService, userDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getUserDto();
    }

    public void activateUser(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("activateUser");
        ActivateUserFunctionalitySagas saga = new ActivateUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void updateUserName(Integer userAggregateId, String name) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateUserName");
        UpdateUserNameFunctionalitySagas saga = new UpdateUserNameFunctionalitySagas(
                unitOfWorkService, userAggregateId, name, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void anonymizeUser(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("anonymizeUser");
        AnonymizeUserFunctionalitySagas saga = new AnonymizeUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteUser(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteUser");
        DeleteUserFunctionalitySagas saga = new DeleteUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
