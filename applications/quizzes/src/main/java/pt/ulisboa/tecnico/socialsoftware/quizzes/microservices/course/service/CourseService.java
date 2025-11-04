package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.sql.SQLException;

@Service
public class CourseService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final CourseCustomRepository courseRepository;

    private final CourseTransactionalService courseTransactionalService = new CourseTransactionalService();

    @Autowired
    private CourseFactory courseFactory;

    public CourseService(UnitOfWorkService unitOfWorkService, CourseCustomRepository courseRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseRepository = courseRepository;
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public CourseDto getCourseById(Integer aggregateId, UnitOfWork unitOfWorkWorkService) {
        return courseTransactionalService.getCourseByIdTransactional(aggregateId, unitOfWorkWorkService,
                unitOfWorkService, courseFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public CourseExecutionDto getAndOrCreateCourseRemote(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
        return courseTransactionalService.getAndOrCreateCourseRemoteTransactional(courseExecutionDto, unitOfWork,
                aggregateIdGeneratorService, courseRepository, unitOfWorkService, courseFactory);
    }
}

@Service
class CourseTransactionalService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto getCourseByIdTransactional(Integer aggregateId, UnitOfWork unitOfWorkWorkService,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, CourseFactory courseFactory) {
        return courseFactory.createCourseDto(
                (Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWorkWorkService));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto getAndOrCreateCourseRemoteTransactional(CourseExecutionDto courseExecutionDto,
            UnitOfWork unitOfWork,
            AggregateIdGeneratorService aggregateIdGeneratorService, CourseCustomRepository courseRepository,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, CourseFactory courseFactory) {
        Course course = getCourseByNameTransactional(courseExecutionDto.getName(), unitOfWork, courseRepository,
                unitOfWorkService);
        if (course == null) {
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            course = courseFactory.createCourse(aggregateId, courseExecutionDto);
            unitOfWorkService.registerChanged(course, unitOfWork);
        }
        courseExecutionDto.setCourseAggregateId(course.getAggregateId());
        courseExecutionDto.setName(course.getName());
        courseExecutionDto.setType(course.getType().toString());
        courseExecutionDto.setCourseVersion(course.getVersion());
        return courseExecutionDto;
    }

    private Course getCourseByNameTransactional(String courseName, UnitOfWork unitOfWork,
            CourseCustomRepository courseRepository, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        return courseRepository.findCourseIdByName(courseName)
                .map(id -> (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .orElse(null);
    }
}
