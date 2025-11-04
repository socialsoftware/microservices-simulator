package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.*;

@Service
public class CourseExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(CourseExecutionService.class);
    private final CourseExecutionRepository courseExecutionRepository;
    private final CourseExecutionCustomRepository courseExecutionCustomRepository;
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private CourseExecutionFactory courseExecutionFactory;

    private final CourseExecutionTransactionalService courseExecutionTransactionalService = new CourseExecutionTransactionalService();

    public CourseExecutionService(UnitOfWorkService unitOfWorkService,
                                  CourseExecutionRepository courseExecutionRepository,
                                  CourseExecutionCustomRepository courseExecutionCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseExecutionRepository = courseExecutionRepository;
        this.courseExecutionCustomRepository = courseExecutionCustomRepository;
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public CourseExecutionDto getCourseExecutionById(Integer executionAggregateId, UnitOfWork unitOfWorkWork) {
        return courseExecutionTransactionalService.getCourseExecutionByIdTransactional(executionAggregateId,
                unitOfWorkWork, unitOfWorkService, courseExecutionFactory);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
        return courseExecutionTransactionalService.createCourseExecutionTransactional(courseExecutionDto, unitOfWork,
                courseService, aggregateIdGeneratorService, courseExecutionFactory, unitOfWorkService);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public List<CourseExecutionDto> getAllCourseExecutions(UnitOfWork unitOfWork) {
        return courseExecutionTransactionalService.getAllCourseExecutionsTransactional(unitOfWork,
                courseExecutionCustomRepository, unitOfWorkService);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void removeCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        courseExecutionTransactionalService.removeCourseExecutionTransactional(executionAggregateId, unitOfWork, this,
                unitOfWorkService, courseExecutionFactory);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void enrollStudent(Integer courseExecutionAggregateId, UserDto userDto, UnitOfWork unitOfWork) {
        courseExecutionTransactionalService.enrollStudentTransactional(courseExecutionAggregateId, userDto, unitOfWork,
                unitOfWorkService, courseExecutionFactory);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public Set<CourseExecutionDto> getCourseExecutionsByUserId(Integer userAggregateId, UnitOfWork unitOfWork) {
        return courseExecutionTransactionalService.getCourseExecutionsByUserIdTransactional(userAggregateId, unitOfWork,
                courseExecutionRepository, unitOfWorkService, courseExecutionFactory);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId,
                                                 UnitOfWork unitOfWork) {
        courseExecutionTransactionalService.removeStudentFromCourseExecutionTransactional(courseExecutionAggregateId,
                userAggregateId, unitOfWork, unitOfWorkService, courseExecutionFactory);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public UserDto getStudentByExecutionIdAndUserId(Integer executionAggregateId, Integer userAggregateId,
                                                    UnitOfWork unitOfWork) {
        return courseExecutionTransactionalService.getStudentByExecutionIdAndUserIdTransactional(executionAggregateId,
                userAggregateId, unitOfWork, unitOfWorkService);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        courseExecutionTransactionalService.anonymizeStudentTransactional(executionAggregateId, userAggregateId,
                unitOfWork, unitOfWorkService, courseExecutionFactory);
    }

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void updateExecutionStudentName(Integer executionAggregateId, Integer userAggregateId, String name,
                                           UnitOfWork unitOfWork) {
        courseExecutionTransactionalService.updateExecutionStudentNameTransactional(executionAggregateId,
                userAggregateId, name, unitOfWork, unitOfWorkService, courseExecutionFactory, logger);
    }

    /************************************************
     * EVENT PROCESSING
     ************************************************/

    @Retryable(retryFor = {
            SQLException.class, TransientDataAccessException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public CourseExecutionDto removeUser(Integer executionAggregateId, Integer userAggregateId,
                                         Integer aggregateEventVersion, UnitOfWork unitOfWork) {
        return courseExecutionTransactionalService.removeUserTransactional(executionAggregateId, userAggregateId,
                aggregateEventVersion, unitOfWork, unitOfWorkService, courseExecutionFactory);
    }
}

@Service
class CourseExecutionTransactionalService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto getCourseExecutionByIdTransactional(Integer executionAggregateId, UnitOfWork unitOfWork,
                                                                  UnitOfWorkService<UnitOfWork> unitOfWorkService, CourseExecutionFactory courseExecutionFactory) {
        return courseExecutionFactory.createCourseExecutionDto(
                (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto createCourseExecutionTransactional(CourseExecutionDto courseExecutionDto,
                                                                 UnitOfWork unitOfWork, CourseService courseService, AggregateIdGeneratorService aggregateIdGeneratorService,
                                                                 CourseExecutionFactory courseExecutionFactory, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        CourseExecutionCourse courseExecutionCourse = new CourseExecutionCourse(
                courseService.getAndOrCreateCourseRemote(courseExecutionDto, unitOfWork));
        CourseExecution courseExecution = courseExecutionFactory.createCourseExecution(
                aggregateIdGeneratorService.getNewAggregateId(), courseExecutionDto, courseExecutionCourse);
        unitOfWorkService.registerChanged(courseExecution, unitOfWork);
        return courseExecutionFactory.createCourseExecutionDto(courseExecution);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<CourseExecutionDto> getAllCourseExecutionsTransactional(UnitOfWork unitOfWork,
                                                                        CourseExecutionCustomRepository courseExecutionCustomRepository,
                                                                        UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        return courseExecutionCustomRepository.findCourseExecutionIdsOfAllNonDeleted().stream()
                .map(id -> (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(CourseExecutionDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeCourseExecutionTransactional(Integer executionAggregateId, UnitOfWork unitOfWork,
                                                   CourseExecutionService courseExecutionService, UnitOfWorkService<UnitOfWork> unitOfWorkService,
                                                   CourseExecutionFactory courseExecutionFactory) {
        CourseExecution oldCourseExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        CourseExecution newCourseExecution = courseExecutionFactory
                .createCourseExecutionFromExisting(oldCourseExecution);

        Integer numberOfExecutionsOfCourse = Math
                .toIntExact(courseExecutionService.getAllCourseExecutions(unitOfWork).stream()
                        .filter(ce -> ce.getCourseAggregateId() == newCourseExecution.getExecutionCourse()
                                .getCourseAggregateId())
                        .count());
        if (numberOfExecutionsOfCourse == 1) {
            throw new QuizzesException(CANNOT_DELETE_COURSE_EXECUTION);
        }

        newCourseExecution.remove();
        unitOfWorkService.registerChanged(newCourseExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteCourseExecutionEvent(newCourseExecution.getAggregateId()),
                unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void enrollStudentTransactional(Integer courseExecutionAggregateId, UserDto userDto, UnitOfWork unitOfWork,
                                           UnitOfWorkService<UnitOfWork> unitOfWorkService, CourseExecutionFactory courseExecutionFactory) {
        CourseExecution oldCourseExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(courseExecutionAggregateId, unitOfWork);

        CourseExecutionStudent courseExecutionStudent = new CourseExecutionStudent(userDto);
        if (!courseExecutionStudent.isActive()) {
            throw new QuizzesException(QuizzesErrorMessage.INACTIVE_USER, courseExecutionStudent.getUserAggregateId());
        }

        if (oldCourseExecution.hasStudent(courseExecutionStudent.getUserAggregateId())) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_ALREADY_ENROLLED,
                    courseExecutionStudent.getUserAggregateId(), courseExecutionAggregateId);
        }

        CourseExecution newCourseExecution = courseExecutionFactory
                .createCourseExecutionFromExisting(oldCourseExecution);
        newCourseExecution.addStudent(courseExecutionStudent);

        unitOfWorkService.registerChanged(newCourseExecution, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Set<CourseExecutionDto> getCourseExecutionsByUserIdTransactional(Integer userAggregateId,
                                                                            UnitOfWork unitOfWork, CourseExecutionRepository courseExecutionRepository,
                                                                            UnitOfWorkService<UnitOfWork> unitOfWorkService, CourseExecutionFactory courseExecutionFactory) {
        return courseExecutionRepository.findAll().stream()
                .map(CourseExecution::getAggregateId)
                .map(aggregateId -> (CourseExecution) unitOfWorkService.aggregateLoad(aggregateId, unitOfWork))
                .filter(ce -> ce.hasStudent(userAggregateId))
                .map(courseExecution -> (CourseExecution) unitOfWorkService.registerRead(courseExecution, unitOfWork))
                .map(ce -> courseExecutionFactory.createCourseExecutionDto(ce))
                .collect(Collectors.toSet());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeStudentFromCourseExecutionTransactional(Integer courseExecutionAggregateId,
                                                              Integer userAggregateId, UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService,
                                                              CourseExecutionFactory courseExecutionFactory) {
        CourseExecution oldCourseExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(courseExecutionAggregateId, unitOfWork);
        CourseExecution newCourseExecution = courseExecutionFactory
                .createCourseExecutionFromExisting(oldCourseExecution);
        newCourseExecution.removeStudent(userAggregateId);
        unitOfWorkService.registerChanged(newCourseExecution, unitOfWork);
        unitOfWorkService.registerEvent(
                new DisenrollStudentFromCourseExecutionEvent(courseExecutionAggregateId, userAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getStudentByExecutionIdAndUserIdTransactional(Integer executionAggregateId, Integer userAggregateId,
                                                                 UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        CourseExecution courseExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        if (!courseExecution.hasStudent(userAggregateId)) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, executionAggregateId);
        }
        return courseExecution.findStudent(userAggregateId).buildDto();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void anonymizeStudentTransactional(Integer executionAggregateId, Integer userAggregateId,
                                              UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService,
                                              CourseExecutionFactory courseExecutionFactory) {
        CourseExecution oldExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        if (!oldExecution.hasStudent(userAggregateId)) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, executionAggregateId);
        }
        CourseExecution newExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).anonymize();
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(
                new AnonymizeStudentEvent(executionAggregateId, "ANONYMOUS", "ANONYMOUS", userAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateExecutionStudentNameTransactional(Integer executionAggregateId, Integer userAggregateId,
                                                        String name, UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService,
                                                        CourseExecutionFactory courseExecutionFactory, Logger logger) {
        CourseExecution oldExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);

        logger.info("Updating student name for user {} in execution {}", userAggregateId, executionAggregateId);

        if (!oldExecution.hasStudent(userAggregateId)) {
            throw new QuizzesException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, executionAggregateId);
        }
        CourseExecution newExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).setName(name);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new UpdateStudentNameEvent(executionAggregateId, userAggregateId, name),
                unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto removeUserTransactional(Integer executionAggregateId, Integer userAggregateId,
                                                      Integer aggregateEventVersion, UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService,
                                                      CourseExecutionFactory courseExecutionFactory) {
        CourseExecution oldExecution = (CourseExecution) unitOfWorkService
                .aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        CourseExecution newExecution = courseExecutionFactory.createCourseExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(
                new DisenrollStudentFromCourseExecutionEvent(executionAggregateId, userAggregateId), unitOfWork);
        return new CourseExecutionDto(newExecution);
    }
}
