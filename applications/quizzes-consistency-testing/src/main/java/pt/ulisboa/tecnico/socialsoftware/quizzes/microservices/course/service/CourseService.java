package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Service
public class CourseService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final CourseCustomRepository courseRepository;

    @Autowired
    private CourseFactory courseFactory;

    public CourseService(UnitOfWorkService unitOfWorkService, CourseCustomRepository courseRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseRepository = courseRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto getCourseById(Integer aggregateId, UnitOfWork unitOfWorkWorkService) {
        return courseFactory.createCourseDto(
                (Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWorkWorkService));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto getAndOrCreateCourseRemote(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
        Course course = getCourseByName(courseExecutionDto.getName(), unitOfWork);
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto getCourseByNameRemote(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
        Course course = getCourseByName(courseExecutionDto.getName(), unitOfWork);
        if (course != null) {
            courseExecutionDto.setCourseAggregateId(course.getAggregateId());
            courseExecutionDto.setName(course.getName());
            courseExecutionDto.setType(course.getType().toString());
            courseExecutionDto.setCourseVersion(course.getVersion());
        }
        // courseAggregateId remains null if course not found
        return courseExecutionDto;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto createCourseRemote(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Course course = courseFactory.createCourse(aggregateId, courseExecutionDto);
        unitOfWorkService.registerChanged(course, unitOfWork);
        courseExecutionDto.setCourseAggregateId(course.getAggregateId());
        courseExecutionDto.setName(course.getName());
        courseExecutionDto.setType(course.getType().toString());
        courseExecutionDto.setCourseVersion(course.getVersion());
        return courseExecutionDto;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteCourse(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course course = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        course.remove();
        unitOfWorkService.registerChanged(course, unitOfWork);
    }

    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void incrementCourseQuestionCount(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
        newCourse.setCourseQuestionCount(newCourse.getCourseQuestionCount() + 1);
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
    }

    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decrementCourseQuestionCount(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
        newCourse.setCourseQuestionCount(Math.max(0, newCourse.getCourseQuestionCount() - 1));
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
    }

    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void incrementCourseExecutionCount(Integer courseAggregateId, UnitOfWork unitOfWork) {
        // The course may have been created in this same UoW (not yet in DB) — update it in-place
        Course pendingCourse = unitOfWork.getAggregatesToCommit().stream()
                .filter(a -> a instanceof Course && courseAggregateId.equals(a.getAggregateId()))
                .map(a -> (Course) a)
                .findFirst()
                .orElse(null);
        if (pendingCourse != null) {
            pendingCourse.setCourseExecutionCount(pendingCourse.getCourseExecutionCount() + 1);
            return;
        }
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
        newCourse.setCourseExecutionCount(newCourse.getCourseExecutionCount() + 1);
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
    }

    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decrementCourseExecutionCount(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
        newCourse.setCourseExecutionCount(Math.max(0, newCourse.getCourseExecutionCount() - 1));
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
    }

    private Course getCourseByName(String courseName, UnitOfWork unitOfWork) {
        return courseRepository.findCourseIdByName(courseName)
                .map(id -> (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .orElse(null);
    }
}
