package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_ACADEMIC_TERM;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_ACRONYM;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_END_DATE;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.AddStudentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.AnonymizeStudentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.CreateCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.GetCourseExecutionByIdFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.GetCourseExecutionsByUserFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.GetCourseExecutionsFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.RemoveCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.RemoveStudentFromCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.UpdateStudentNameFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.AddStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.AnonymizeStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CreateCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetCourseExecutionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetCourseExecutionsByUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetCourseExecutionsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.RemoveCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.RemoveStudentFromCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateStudentNameFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class CourseExecutionFunctionalities {
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private UserService userService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private CourseExecutionFactory courseExecutionFactory;

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

    public CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(courseExecutionDto);
            CreateCourseExecutionFunctionalitySagas functionality = new CreateCourseExecutionFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, courseExecutionDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            
            return functionality.getCreatedCourseExecution();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(courseExecutionDto);
            CreateCourseExecutionFunctionalityTCC functionality = new CreateCourseExecutionFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, courseExecutionDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);

            return functionality.getCreatedCourseExecution();
        }
    }

    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetCourseExecutionByIdFunctionalitySagas functionality = new GetCourseExecutionByIdFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, executionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCourseExecutionDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetCourseExecutionByIdFunctionalityTCC functionality = new GetCourseExecutionByIdFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, executionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCourseExecutionDto();
        }
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetCourseExecutionsFunctionalitySagas functionality = new GetCourseExecutionsFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCourseExecutions();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetCourseExecutionsFunctionalityTCC functionality = new GetCourseExecutionsFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCourseExecutions();
        }
    }

    public void removeCourseExecution(Integer executionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveCourseExecutionFunctionalitySagas functionality = new RemoveCourseExecutionFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, executionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveCourseExecutionFunctionalityTCC functionality = new RemoveCourseExecutionFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, executionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            AddStudentFunctionalitySagas functionality = new AddStudentFunctionalitySagas(
                    courseExecutionService, userService, sagaUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            AddStudentFunctionalityTCC functionality = new AddStudentFunctionalityTCC(
                    courseExecutionService, userService, causalUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetCourseExecutionsByUserFunctionalitySagas functionality = new GetCourseExecutionsByUserFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCourseExecutions();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetCourseExecutionsByUserFunctionalityTCC functionality = new GetCourseExecutionsByUserFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCourseExecutions();
        }
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveStudentFromCourseExecutionFunctionalitySagas functionality = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, courseExecutionFactory, courseExecutionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveStudentFromCourseExecutionFunctionalityTCC functionality = new RemoveStudentFromCourseExecutionFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, courseExecutionFactory, courseExecutionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            AnonymizeStudentFunctionalitySagas functionality = new AnonymizeStudentFunctionalitySagas(
                    courseExecutionService, sagaUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            AnonymizeStudentFunctionalityTCC functionality = new AnonymizeStudentFunctionalityTCC(
                    courseExecutionService, causalUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId, UserDto userDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateStudentNameFunctionalitySagas functionality = new UpdateStudentNameFunctionalitySagas(
                    courseExecutionService, courseExecutionFactory, sagaUnitOfWorkService, executionAggregateId, userAggregateId, userDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateStudentNameFunctionalityTCC functionality = new UpdateStudentNameFunctionalityTCC(
                    courseExecutionService, courseExecutionFactory, causalUnitOfWorkService, executionAggregateId, userAggregateId, userDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }
    

    private void checkInput(CourseExecutionDto courseExecutionDto) {
        if (courseExecutionDto.getAcronym() == null) {
            throw new TutorException(COURSE_EXECUTION_MISSING_ACRONYM);
        }
        if (courseExecutionDto.getAcademicTerm() == null) {
            throw new TutorException(COURSE_EXECUTION_MISSING_ACADEMIC_TERM);
        }
        if (courseExecutionDto.getEndDate() == null) {
            throw new TutorException(COURSE_EXECUTION_MISSING_END_DATE);
        }

    }

}
