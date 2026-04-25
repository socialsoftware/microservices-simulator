package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;

@Service
public class CourseService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private CourseFactory courseFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final CourseCustomRepository courseRepository;

    public CourseService(UnitOfWorkService unitOfWorkService, CourseCustomRepository courseRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseRepository = courseRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto getCourseById(Integer aggregateId, UnitOfWork unitOfWork) {
        return courseFactory.createCourseDto(
                (Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto createCourse(String name, String type, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Course course = courseFactory.createCourse(aggregateId, name, type);
        unitOfWorkService.registerChanged(course, unitOfWork);
        return courseFactory.createCourseDto(course);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateCourse(Integer courseAggregateId, String name, String type, UnitOfWork unitOfWork) {
        // COURSE_NAME_FINAL and COURSE_TYPE_FINAL are P1 invariants (final fields) — update is not permitted
        throw new QuizzesFullException(QuizzesFullErrorMessage.COURSE_FIELDS_IMMUTABLE);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteCourse(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course course = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        course.remove();
        unitOfWorkService.registerChanged(course, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decrementExecutionCount(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseCopy(oldCourse);
        newCourse.setExecutionCount(Math.max(0, newCourse.getExecutionCount() - 1));
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decrementQuestionCount(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Course newCourse = courseFactory.createCourseCopy(oldCourse);
        newCourse.setQuestionCount(Math.max(0, newCourse.getQuestionCount() - 1));
        unitOfWorkService.registerChanged(newCourse, unitOfWork);
    }
}
