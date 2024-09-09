package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.ActivateUserFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.CreateUserFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.DeleteUserFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.FindUserByIdFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.GetStudentsFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.GetTeachersFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.ActivateUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CreateUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.DeleteUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindUserByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetStudentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetTeachersFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class UserFunctionalities {
    @Autowired
    private UserService userService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Autowired
    private Environment env;

    private String workflowType;

    @PostConstruct
    public void init() {
        // Determine the workflow type based on active profiles
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains("sagas")) {
            workflowType = "sagas";
        } else if (Arrays.asList(activeProfiles).contains("tcc")) {
            workflowType = "tcc";
        } else {
            workflowType = "unknown"; // Default or fallback value
        }
    }

    public UserDto createUser(UserDto userDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(userDto);

            CreateUserFunctionalitySagas functionality = new CreateUserFunctionalitySagas(
                    userService, sagaUnitOfWorkService, userDto, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedUserDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(userDto);

            CreateUserFunctionalityTCC functionality = new CreateUserFunctionalityTCC(
                    userService, causalUnitOfWorkService, userDto, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedUserDto();
        }
    }

    public UserDto findByUserId(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            FindUserByIdFunctionalitySagas functionality = new FindUserByIdFunctionalitySagas(
                    userService, sagaUnitOfWorkService, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getUserDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindUserByIdFunctionalityTCC functionality = new FindUserByIdFunctionalityTCC(
                    userService, causalUnitOfWorkService, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getUserDto();
        }
    }

    public void activateUser(Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            ActivateUserFunctionalitySagas functionality = new ActivateUserFunctionalitySagas(
                    userService, sagaUnitOfWorkService, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            ActivateUserFunctionalityTCC functionality = new ActivateUserFunctionalityTCC(
                    userService, causalUnitOfWorkService, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void deleteUser(Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            DeleteUserFunctionalitySagas functionality = new DeleteUserFunctionalitySagas(
                    userService, sagaUnitOfWorkService, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            DeleteUserFunctionalityTCC functionality = new DeleteUserFunctionalityTCC(
                    userService, causalUnitOfWorkService, userAggregateId, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
        }
    }

    public List<UserDto> getStudents() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetStudentsFunctionalitySagas functionality = new GetStudentsFunctionalitySagas(
                    userService, sagaUnitOfWorkService, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getStudents();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetStudentsFunctionalityTCC functionality = new GetStudentsFunctionalityTCC(
                    userService, causalUnitOfWorkService, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getStudents();
        }
    }

    public List<UserDto> getTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetTeachersFunctionalitySagas functionality = new GetTeachersFunctionalitySagas(
                    userService, sagaUnitOfWorkService, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTeachers();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetTeachersFunctionalityTCC functionality = new GetTeachersFunctionalityTCC(
                    userService, causalUnitOfWorkService, unitOfWork);

            functionality.executeWorkflow(unitOfWork);
            return functionality.getTeachers();
        }
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
