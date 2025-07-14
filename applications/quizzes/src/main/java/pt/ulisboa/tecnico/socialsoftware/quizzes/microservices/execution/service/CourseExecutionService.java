package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.CANNOT_DELETE_COURSE_EXECUTION;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.COURSE_EXECUTION_STUDENT_ALREADY_ENROLLED;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

@Service
public class CourseExecutionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;
    
    @Autowired
    private CourseService courseService;

    private final CourseExecutionRepository courseExecutionRepository;

    private final CourseExecutionCustomRepository courseExecutionCustomRepository;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private CourseExecutionFactory courseExecutionFactory;

    private static final Logger logger = LoggerFactory.getLogger(CourseExecutionService.class);

    public CourseExecutionService(UnitOfWorkService unitOfWorkService, CourseExecutionRepository courseExecutionRepository, CourseExecutionCustomRepository courseExecutionCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseExecutionRepository = courseExecutionRepository;
        this.courseExecutionCustomRepository = courseExecutionCustomRepository;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto getCourseExecutionById(Integer executionAggregateId, UnitOfWork unitOfWorkWork) {
        return courseExecutionFactory.createCourseExecutionDto((CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWorkWork));
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
        CourseExecutionCourse courseExecutionCourse = new CourseExecutionCourse(courseService.getAndOrCreateCourseRemote(courseExecutionDto, unitOfWork));

        CourseExecution courseExecution = courseExecutionFactory.createCourseExecution(aggregateIdGeneratorService.getNewAggregateId(), courseExecutionDto, courseExecutionCourse);

        unitOfWorkService.registerChanged(courseExecution, unitOfWork);
        return courseExecutionFactory.createCourseExecutionDto(courseExecution);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<CourseExecutionDto> getAllCourseExecutions(UnitOfWork unitOfWork) {
        return courseExecutionCustomRepository.findCourseExecutionIdsOfAllNonDeleted().stream()
                .map(id -> (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(CourseExecutionDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        CourseExecution oldCourseExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldCourseExecution);

        /*
            REMOVE_COURSE_IS_VALID
         */
        Integer numberOfExecutionsOfCourse = Math.toIntExact(getAllCourseExecutions(unitOfWork).stream()
                .filter(ce -> ce.getCourseAggregateId() == newCourseExecution.getExecutionCourse().getCourseAggregateId())
                .count());
        if (numberOfExecutionsOfCourse == 1) {
            throw new QuizzesException(CANNOT_DELETE_COURSE_EXECUTION);
        }

        newCourseExecution.remove();
        unitOfWorkService.registerChanged(newCourseExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteCourseExecutionEvent(newCourseExecution.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void enrollStudent(Integer courseExecutionAggregateId, UserDto userDto, UnitOfWork unitOfWork) {
        CourseExecution oldCourseExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionAggregateId, unitOfWork);

        CourseExecutionStudent courseExecutionStudent = new CourseExecutionStudent(userDto);
        if (!courseExecutionStudent.isActive()){
            throw new QuizzesException(QuizzesErrorMessage.INACTIVE_USER, courseExecutionStudent.getUserAggregateId());
        }
        
        if (oldCourseExecution.hasStudent(courseExecutionStudent.getUserAggregateId())) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_ALREADY_ENROLLED, courseExecutionStudent.getUserAggregateId(), courseExecutionAggregateId);
        }

        CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldCourseExecution);
        newCourseExecution.addStudent(courseExecutionStudent);

        unitOfWorkService.registerChanged(newCourseExecution, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Set<CourseExecutionDto> getCourseExecutionsByUserId(Integer userAggregateId, UnitOfWork unitOfWork) {
        return courseExecutionRepository.findAll().stream()
                .map(CourseExecution::getAggregateId)
                .map(aggregateId -> (CourseExecution) unitOfWorkService.aggregateLoad(aggregateId, unitOfWork))
                .filter(ce -> ce.hasStudent(userAggregateId))
                .map(courseExecution -> (CourseExecution) unitOfWorkService.registerRead(courseExecution, unitOfWork))
                .map(ce -> courseExecutionFactory.createCourseExecutionDto(ce))
                .collect(Collectors.toSet());
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        CourseExecution oldCourseExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionAggregateId, unitOfWork);
        CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldCourseExecution);
        newCourseExecution.removeStudent(userAggregateId);
        unitOfWorkService.registerChanged(newCourseExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DisenrollStudentFromCourseExecutionEvent(courseExecutionAggregateId, userAggregateId), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getStudentByExecutionIdAndUserId(Integer executionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        CourseExecution courseExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        if (!courseExecution.hasStudent(userAggregateId)) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, executionAggregateId);
        }
        return courseExecution.findStudent(userAggregateId).buildDto();
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        CourseExecution oldExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        if (!oldExecution.hasStudent(userAggregateId)) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, executionAggregateId);
        }
        CourseExecution newExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).anonymize();
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new AnonymizeStudentEvent(executionAggregateId, "ANONYMOUS", "ANONYMOUS", userAggregateId), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateExecutionStudentName(Integer executionAggregateId, Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        CourseExecution oldExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        
        logger.info("Updating student name for user {} in execution {}", userAggregateId, executionAggregateId);

        if (!oldExecution.hasStudent(userAggregateId)) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, executionAggregateId);
        }
        CourseExecution newExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).setName(name);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new UpdateStudentNameEvent(executionAggregateId, userAggregateId, name), unitOfWork);
    }

    /************************************************ EVENT PROCESSING ************************************************/

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecution removeUser(Integer executionAggregateId, Integer userAggregateId, Integer aggregateEventVersion, UnitOfWork unitOfWork) {
        CourseExecution oldExecution = (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        CourseExecution newExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DisenrollStudentFromCourseExecutionEvent(executionAggregateId, userAggregateId), unitOfWork);
        return newExecution;
    }
}
