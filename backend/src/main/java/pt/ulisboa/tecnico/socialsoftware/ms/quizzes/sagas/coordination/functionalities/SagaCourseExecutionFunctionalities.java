package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_ACADEMIC_TERM;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_ACRONYM;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_END_DATE;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.USER_MISSING_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaUser;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.AddStudentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.AnonymizeStudentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetCourseExecutionByIdData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetCourseExecutionsByUserData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetCourseExecutionsData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.RemoveCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.RemoveStudentFromCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateStudentNameData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

@Profile("sagas")
@Service
public class SagaCourseExecutionFunctionalities implements CourseExecutionFunctionalitiesInterface {
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private UserService userService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CourseExecutionFactory courseExecutionFactory;

    public CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        CreateCourseExecutionData data = new CreateCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);

        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(courseExecutionDto);
            data.setCourseExecutionDto(courseExecutionDto);
        });

        SyncStep createCourseExecutionStep = new SyncStep(() -> {
            CourseExecutionDto createdCourseExecution = courseExecutionService.createCourseExecution(data.getCourseExecutionDto(), unitOfWork);
            data.setCreatedCourseExecution(createdCourseExecution);
        }, new ArrayList<>(Arrays.asList(checkInputStep)));

        createCourseExecutionStep.registerCompensation(() -> {
            CourseExecution courseExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(data.getCreatedCourseExecution().getAggregateId(), unitOfWork);
            courseExecution.remove();
            unitOfWork.registerChanged(courseExecution);
        }, unitOfWork);

        workflow.addStep(checkInputStep);
        workflow.addStep(createCourseExecutionStep);

        workflow.execute(unitOfWork);

        return data.getCreatedCourseExecution();
    }


    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionByIdData data = new GetCourseExecutionByIdData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getCourseExecutionStep = new SyncStep(() -> {
            CourseExecutionDto courseExecutionDto = courseExecutionService.getCourseExecutionById(executionAggregateId, unitOfWork);
            data.setCourseExecutionDto(courseExecutionDto);
        });
    
        workflow.addStep(getCourseExecutionStep);
        workflow.execute(unitOfWork);
    
        return data.getCourseExecutionDto();
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionsData data = new GetCourseExecutionsData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getCourseExecutionsStep = new SyncStep(() -> {
            List<CourseExecutionDto> courseExecutions = courseExecutionService.getAllCourseExecutions(unitOfWork);
            data.setCourseExecutions(courseExecutions);
        });
    
        workflow.addStep(getCourseExecutionsStep);
        workflow.execute(unitOfWork);
    
        return data.getCourseExecutions();
    }

    public void removeCourseExecution(Integer executionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveCourseExecutionData data = new RemoveCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getCourseExecutionStep = new SyncStep(() -> {
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, SagaState.IN_SAGA, unitOfWork);
            data.setCourseExecution(courseExecution);
        });
    
        getCourseExecutionStep.registerCompensation(() -> {
            SagaCourseExecution courseExecution = data.getCourseExecution();
            unitOfWorkService.registerSagaState(courseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(courseExecution);
        }, unitOfWork);
    
        SyncStep removeCourseExecutionStep = new SyncStep(() -> {
            courseExecutionService.removeCourseExecution(executionAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));
    
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(removeCourseExecutionStep);
    
        workflow.execute(unitOfWork);
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        AddStudentData data = new AddStudentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getUserStep = new SyncStep(() -> {
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
            data.setUserDto(userDto);
        });
    
        SyncStep getOldCourseExecutionStep = new SyncStep(() -> {
            SagaCourseExecution oldCourseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldCourseExecution, SagaState.IN_SAGA, unitOfWork);
            data.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
            CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(data.getOldCourseExecution());
            unitOfWorkService.registerSagaState((SagaCourseExecution) newCourseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newCourseExecution);
        }, unitOfWork);
    
        SyncStep enrollStudentStep = new SyncStep(() -> {
            courseExecutionService.enrollStudent(executionAggregateId, data.getUserDto(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep, getOldCourseExecutionStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(enrollStudentStep);
    
        workflow.execute(unitOfWork);
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionsByUserData data = new GetCourseExecutionsByUserData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getCourseExecutionsByUserStep = new SyncStep(() -> {
            Set<CourseExecutionDto> courseExecutions = courseExecutionService.getCourseExecutionsByUserId(userAggregateId, unitOfWork);
            data.setCourseExecutions(courseExecutions);
        });
    
        workflow.addStep(getCourseExecutionsByUserStep);
        workflow.execute(unitOfWork);
    
        return data.getCourseExecutions();
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveStudentFromCourseExecutionData data = new RemoveStudentFromCourseExecutionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getOldCourseExecutionStep = new SyncStep(() -> {
            SagaCourseExecution oldCourseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldCourseExecution, SagaState.IN_SAGA, unitOfWork);
            data.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
            CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(data.getOldCourseExecution());
            unitOfWorkService.registerSagaState((SagaCourseExecution) newCourseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newCourseExecution);
        }, unitOfWork);
    
        SyncStep removeStudentStep = new SyncStep(() -> {
            courseExecutionService.removeStudentFromCourseExecution(courseExecutionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldCourseExecutionStep)));
    
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(removeStudentStep);
    
        workflow.execute(unitOfWork);
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        AnonymizeStudentData data = new AnonymizeStudentData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getOldCourseExecutionStep = new SyncStep(() -> {
            SagaCourseExecution oldCourseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldCourseExecution, SagaState.IN_SAGA, unitOfWork);
            data.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
            CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(data.getOldCourseExecution());
            unitOfWorkService.registerSagaState((SagaCourseExecution) newCourseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newCourseExecution);
        }, unitOfWork);
    
        SyncStep anonymizeStudentStep = new SyncStep(() -> {
            courseExecutionService.anonymizeStudent(executionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldCourseExecutionStep)));
    
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(anonymizeStudentStep);
    
        workflow.execute(unitOfWork);
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId, UserDto userDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateStudentNameData data = new UpdateStudentNameData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        if (userDto.getName() == null) {
            throw new TutorException(USER_MISSING_NAME);
        }
    
        SyncStep getStudentStep = new SyncStep(() -> {
            SagaUser student = (SagaUser) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(student, SagaState.IN_SAGA, unitOfWork);
            data.setStudent(student);
        });
    
        getStudentStep.registerCompensation(() -> {
            SagaUser student = data.getStudent();
            unitOfWorkService.registerSagaState(student, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(student);
        }, unitOfWork);
    
        SyncStep getOldCourseExecutionStep = new SyncStep(() -> {
            SagaCourseExecution oldCourseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldCourseExecution, SagaState.IN_SAGA, unitOfWork);
            data.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
            CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(data.getOldCourseExecution());
            unitOfWorkService.registerSagaState((SagaCourseExecution) newCourseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newCourseExecution);
        }, unitOfWork);
    
        SyncStep updateStudentNameStep = new SyncStep(() -> {
            courseExecutionService.updateExecutionStudentName(executionAggregateId, userAggregateId, userDto.getName(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getStudentStep, getOldCourseExecutionStep)));
    
        workflow.addStep(getStudentStep);
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(updateStudentNameStep);
    
        workflow.execute(unitOfWork);
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
