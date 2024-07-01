package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.ActivateUserData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateUserData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.DeleteUserData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindUserByIdData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetStudentsData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetTeachersData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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

        CreateUserData data = new CreateUserData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);

        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(userDto);
            userDto.setState(AggregateState.IN_SAGA.toString());
            data.setUserDto(userDto);
        });

        SyncStep createUserStep = new SyncStep(() -> {
            UserDto createdUserDto = userService.createUser(data.getUserDto(), unitOfWork);
            data.setCreatedUserDto(createdUserDto);
        }, new ArrayList<>(Arrays.asList(checkInputStep)));

        createUserStep.registerCompensation(() -> {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(data.getCreatedUserDto().getAggregateId(), unitOfWork);
            user.remove();
            unitOfWork.registerChanged(user);
        }, unitOfWork);

        workflow.addStep(checkInputStep);
        workflow.addStep(createUserStep);

        workflow.execute();

        return data.getCreatedUserDto();
    }

    public UserDto findByUserId(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindUserByIdData data = new FindUserByIdData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep findUserStep = new SyncStep(() -> {
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
            data.setUserDto(userDto);
        });
    
        workflow.addStep(findUserStep);
        workflow.execute();
    
        return data.getUserDto();
    }

    public void activateUser(Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        ActivateUserData data = new ActivateUserData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getUserStep = new SyncStep(() -> {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
            user.setState(AggregateState.IN_SAGA);
            data.setUser(user);
        });
    
        getUserStep.registerCompensation(() -> {
            User user = data.getUser();
            user.setActive(false);
            user.setState(AggregateState.ACTIVE);
            unitOfWork.registerChanged(user);
        }, unitOfWork);
    
        SyncStep activateUserStep = new SyncStep(() -> {
            userService.activateUser(userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(activateUserStep);
    
        workflow.execute();
    }

    public void deleteUser(Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        DeleteUserData data = new DeleteUserData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getUserStep = new SyncStep(() -> {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
            user.setState(AggregateState.IN_SAGA);
            data.setUser(user);
        });
    
        getUserStep.registerCompensation(() -> {
            User user = data.getUser();
            user.setState(AggregateState.ACTIVE);
            unitOfWork.registerChanged(user);
        }, unitOfWork);
    
        SyncStep deleteUserStep = new SyncStep(() -> {
            userService.deleteUser(userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(deleteUserStep);
    
        workflow.execute();
    }

    public List<UserDto> getStudents() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetStudentsData data = new GetStudentsData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getStudentsStep = new SyncStep(() -> {
            List<UserDto> students = userService.getStudents(unitOfWork);
            data.setStudents(students);
        });
    
        workflow.addStep(getStudentsStep);
        workflow.execute();
    
        return data.getStudents();
    }

    public List<UserDto> getTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTeachersData data = new GetTeachersData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getTeachersStep = new SyncStep(() -> {
            List<UserDto> teachers = userService.getTeachers(unitOfWork);
            data.setTeachers(teachers);
        });
    
        workflow.addStep(getTeachersStep);
        workflow.execute();
    
        return data.getTeachers();
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
