package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;


import java.util.List;

@Profile("sagas")
@Service
public class SagaUserFunctionalities implements UserFunctionalitiesInterface {
    @Autowired
    private UserService userService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public UserDto createUser(UserDto userDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            checkInput(userDto);
            UserDto createdUserDto = userService.createUser(userDto, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> userService.deleteUser(createdUserDto.getAggregateId(), unitOfWork));

            unitOfWorkService.commit(unitOfWork);
            return createdUserDto;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error creating user", ex);
        }
    }

    public UserDto findByUserId(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return userService.getUserById(userAggregateId, unitOfWork);
    }

    public void activateUser(Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            userService.activateUser(userAggregateId, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> userService.deactivateUser(userAggregateId, unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error activating user", ex);
        }
    }

    public void deleteUser(Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            userService.deleteUser(userAggregateId, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> userService.createUser(userAggregateId, unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error deleting user", ex);
        }
    }

    public List<UserDto> getStudents() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return userService.getStudents(unitOfWork);
    }

    public List<UserDto> getTeachers() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return userService.getTeachers(unitOfWork);
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
