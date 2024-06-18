package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;

import java.util.List;

@Profile("tcc")
@Service
public class CausalUserFunctionalities implements UserFunctionalitiesInterface {
    @Autowired
    private UserService userService;
    @Autowired
    private CausalUnitOfWorkService unitOfWorkService;

    public UserDto createUser(UserDto userDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        checkInput(userDto);

        UserDto userDto1 = userService.createUser(userDto, unitOfWork);

        unitOfWorkService.commit(unitOfWork);

        return userDto1;
    }

    public UserDto findByUserId(Integer userAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return userService.getUserById(userAggregateId, unitOfWork);
    }

    public void activateUser(Integer userAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        userService.activateUser(userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void deleteUser(Integer userAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        userService.deleteUser(userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public List<UserDto> getStudents() {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return userService.getStudents(unitOfWork);
    }

    public List<UserDto> getTeachers() {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
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