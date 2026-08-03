package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.GetStudentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.GetTeachersFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.GetUserByIdFunctionalitySagas;

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
}
