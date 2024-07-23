package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_ACADEMIC_TERM;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_ACRONYM;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.COURSE_EXECUTION_MISSING_END_DATE;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.AddStudentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.AnonymizeStudentData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetCourseExecutionByIdData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetCourseExecutionsByUserData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetCourseExecutionsData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.RemoveCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.RemoveStudentFromCourseExecutionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateStudentNameData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

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

        checkInput(courseExecutionDto);

        CreateCourseExecutionData data = new CreateCourseExecutionData(courseExecutionService, unitOfWorkService, courseExecutionDto, unitOfWork);

        data.executeWorkflow(unitOfWork);
        return data.getCreatedCourseExecution();
    }


    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionByIdData data = new GetCourseExecutionByIdData(courseExecutionService, unitOfWorkService, executionAggregateId, unitOfWork);

        data.executeWorkflow(unitOfWork);
        return data.getCourseExecutionDto();
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionsData data = new GetCourseExecutionsData(courseExecutionService, unitOfWorkService, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getCourseExecutions();
    }

    public void removeCourseExecution(Integer executionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveCourseExecutionData data = new RemoveCourseExecutionData(courseExecutionService, unitOfWorkService, executionAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        AddStudentData data = new AddStudentData(courseExecutionService, userService, unitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionsByUserData data = new GetCourseExecutionsByUserData(courseExecutionService, unitOfWorkService, userAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getCourseExecutions();
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveStudentFromCourseExecutionData data = new RemoveStudentFromCourseExecutionData(courseExecutionService, unitOfWorkService, courseExecutionFactory, userAggregateId, userAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        AnonymizeStudentData data = new AnonymizeStudentData(courseExecutionService, unitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId, UserDto userDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateStudentNameData data = new UpdateStudentNameData(courseExecutionService, courseExecutionFactory, unitOfWorkService, executionAggregateId, userAggregateId, userDto, unitOfWork);
    
        data.executeWorkflow(unitOfWork);
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
