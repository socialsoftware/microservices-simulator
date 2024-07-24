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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.AddStudentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.AnonymizeStudentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CreateCourseExecutionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetCourseExecutionByIdFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetCourseExecutionsByUserFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetCourseExecutionsFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.RemoveCourseExecutionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.RemoveStudentFromCourseExecutionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateStudentNameFunctionality;
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

        CreateCourseExecutionFunctionality functionality = new CreateCourseExecutionFunctionality(courseExecutionService, unitOfWorkService, courseExecutionDto, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
        return functionality.getCreatedCourseExecution();
    }


    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionByIdFunctionality functionality = new GetCourseExecutionByIdFunctionality(courseExecutionService, unitOfWorkService, executionAggregateId, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
        return functionality.getCourseExecutionDto();
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionsFunctionality functionality = new GetCourseExecutionsFunctionality(courseExecutionService, unitOfWorkService, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getCourseExecutions();
    }

    public void removeCourseExecution(Integer executionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveCourseExecutionFunctionality functionality = new RemoveCourseExecutionFunctionality(courseExecutionService, unitOfWorkService, executionAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        AddStudentFunctionality functionality = new AddStudentFunctionality(courseExecutionService, userService, unitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetCourseExecutionsByUserFunctionality functionality = new GetCourseExecutionsByUserFunctionality(courseExecutionService, unitOfWorkService, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getCourseExecutions();
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveStudentFromCourseExecutionFunctionality functionality = new RemoveStudentFromCourseExecutionFunctionality(courseExecutionService, unitOfWorkService, courseExecutionFactory, userAggregateId, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        AnonymizeStudentFunctionality functionality = new AnonymizeStudentFunctionality(courseExecutionService, unitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId, UserDto userDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateStudentNameFunctionality functionality = new UpdateStudentNameFunctionality(courseExecutionService, courseExecutionFactory, unitOfWorkService, executionAggregateId, userAggregateId, userDto, unitOfWork);
    
        functionality.executeWorkflow(unitOfWork);
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
