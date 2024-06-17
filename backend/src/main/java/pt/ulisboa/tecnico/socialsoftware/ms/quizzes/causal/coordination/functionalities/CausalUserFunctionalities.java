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
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import java.util.ArrayList;
import java.util.List;

@Profile("tcc")
@Service
public class CausalUserFunctionalities implements UserFunctionalitiesInterface {
    @Autowired
    private UserService userService;
    @Autowired
    private CausalUnitOfWorkService unitOfWorkService;

    public UserDto createUser(UserDto userDto) {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CausalWorkflow workflow = new CausalWorkflow(unitOfWorkService, functionalityName);

        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(userDto);
        });

        final UserDto[] userDtoHolder = new UserDto[1];
        
        SyncStep createUserStep = new SyncStep(() -> {
            userDtoHolder[0] = userService.createUser(userDto, unitOfWork);
        });

        workflow.addStep(checkInputStep);
        workflow.addStep(createUserStep);

        workflow.execute();

        return userDtoHolder[0];
    }

    public UserDto findByUserId(Integer userAggregateId) {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CausalWorkflow workflow = new CausalWorkflow(unitOfWorkService, functionalityName);
    
        final UserDto[] userDtoHolder = new UserDto[1];
    
        SyncStep findUserStep = new SyncStep(() -> {
            userDtoHolder[0] = userService.getUserById(userAggregateId, unitOfWork);
        });
    
        workflow.addStep(findUserStep);
    
        workflow.execute();
    
        return userDtoHolder[0];
    }   

    public void activateUser(Integer userAggregateId) {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CausalWorkflow workflow = new CausalWorkflow(unitOfWorkService, functionalityName);
    
        SyncStep activateUserStep = new SyncStep(() -> {
            userService.activateUser(userAggregateId, unitOfWork);
        });
    
        workflow.addStep(activateUserStep);
    
        workflow.execute();
    }

    public void deleteUser(Integer userAggregateId) {

        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CausalWorkflow workflow = new CausalWorkflow(unitOfWorkService, functionalityName);
    
        SyncStep deleteUserStep = new SyncStep(() -> {
            userService.deleteUser(userAggregateId, unitOfWork);
        });
    
        workflow.addStep(deleteUserStep);
    
        workflow.execute();
    }

    public List<UserDto> getStudents() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CausalWorkflow workflow = new CausalWorkflow(unitOfWorkService, functionalityName);

        List<UserDto> studentsHolder = new ArrayList<>();

        SyncStep getStudentsStep = new SyncStep(() -> {
            studentsHolder.addAll(userService.getStudents(unitOfWork));
        });

        workflow.addStep(getStudentsStep);

        workflow.execute();

        return studentsHolder;
    }

    public List<UserDto> getTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CausalWorkflow workflow = new CausalWorkflow(unitOfWorkService, functionalityName);
    
        List<UserDto> teachersHolder = new ArrayList<>();
    
        SyncStep getTeachersStep = new SyncStep(() -> {
            teachersHolder.addAll(userService.getTeachers(unitOfWork));
        });
    
        workflow.addStep(getTeachersStep);
    
        workflow.execute();
    
        return teachersHolder;
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
