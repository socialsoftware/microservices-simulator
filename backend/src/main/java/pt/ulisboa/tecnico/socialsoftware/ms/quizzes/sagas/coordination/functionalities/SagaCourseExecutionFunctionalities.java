package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

import java.util.List;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

@Profile("sagas")
@Service
public class SagaCourseExecutionFunctionalities implements CourseExecutionFunctionalitiesInterface {
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private UserService userService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            checkInput(courseExecutionDto);
            CourseExecutionDto createdCourseExecution = courseExecutionService.createCourseExecution(courseExecutionDto, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> courseExecutionService.removeCourseExecution(createdCourseExecution.getAggregateId(), unitOfWork));

            unitOfWorkService.commit(unitOfWork);
            return createdCourseExecution;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error creating course execution", ex);
        }
    }

    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return courseExecutionService.getCourseExecutionById(executionAggregateId, unitOfWork);
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return courseExecutionService.getAllCourseExecutions(unitOfWork);
    }

    public void removeCourseExecution(Integer executionAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());

        try {
            //TODO check this
            // unitOfWork.registerCompensation(() -> courseExecutionService.createCourseExecution(courseExecutionService.getCourseExecutionById(executionAggregateId, unitOfWork), unitOfWork));

            courseExecutionService.removeCourseExecution(executionAggregateId, unitOfWork);
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error removing course execution", ex);
        }
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());

        try {
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
            courseExecutionService.enrollStudent(executionAggregateId, userDto, unitOfWork);
            
            // TODO
            // unitOfWork.registerCompensation(() -> courseExecutionService.removeStudentFromCourseExecution(executionAggregateId, userAggregateId, unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error adding student to course execution", ex);
        }
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return courseExecutionService.getCourseExecutionsByUserId(userAggregateId, unitOfWork);
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());

        try {
            courseExecutionService.removeStudentFromCourseExecution(courseExecutionAggregateId, userAggregateId, unitOfWork);
            
            // TODO
            // unitOfWork.registerCompensation(() -> courseExecutionService.addStudentToCourseExecution(executionAggregateId, userAggregateId, unitOfWork));
            
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error removing student from course execution", ex);
        }
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            courseExecutionService.anonymizeStudent(executionAggregateId, userAggregateId, unitOfWork);
            // TODO register compensate with revert anonymize
            // unitOfWork.registerCompensation(() -> courseExecutionService.deanonimize(executionAggregateId, userAggregateId, unitOfWork));
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error anonymizing student", ex);
        }
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId , UserDto userDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());

        if (userDto.getName() == null) {
            throw new TutorException(USER_MISSING_NAME);
        }

        try {
            courseExecutionService.updateExecutionStudentName(executionAggregateId, userAggregateId, userDto.getName(), unitOfWork);
            //TODO register compensate with revert with previous name
            // unitOfWork.registerCompensation(() ->
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error updating student name", ex);
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
