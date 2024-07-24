package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.ActivateUserFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CreateUserFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.DeleteUserFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindUserByIdFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetStudentsFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetTeachersFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Profile("sagas")
@Service
public class SagaUserFunctionalities implements UserFunctionalitiesInterface {
    @Autowired
    private UserService userService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public UserDto createUser(UserDto userDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        checkInput(userDto);

        CreateUserFunctionality functionality = new CreateUserFunctionality(userService, unitOfWorkService, userDto, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
        return functionality.getCreatedUserDto();
    }

    public UserDto findByUserId(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindUserByIdFunctionality functionality = new FindUserByIdFunctionality(userService, unitOfWorkService, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getUserDto();
    }

    public void activateUser(Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        ActivateUserFunctionality functionality = new ActivateUserFunctionality(userService, unitOfWorkService, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public void deleteUser(Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        DeleteUserFunctionality functionality = new DeleteUserFunctionality(userService, unitOfWorkService, userAggregateId, unitOfWork);
       
        functionality.executeWorkflow(unitOfWork);
    }

    public List<UserDto> getStudents() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetStudentsFunctionality functionality = new GetStudentsFunctionality(userService, unitOfWorkService, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getStudents();
    }

    public List<UserDto> getTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTeachersFunctionality functionality = new GetTeachersFunctionality(userService, unitOfWorkService, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getTeachers();
    }

    private void checkInput(UserDto userDto) {
        if (userDto.getName() == null) {
            throw new TutorException(ErrorMessage.USER_MISSING_NAME);
        }
        if (userDto.getUsername() == null) {
            throw new TutorException(ErrorMessage.USER_MISSING_USERNAME);
        }
        if (userDto.getRole() == null) {
            throw new TutorException(ErrorMessage.USER_MISSING_ROLE);
        }
    }
}
