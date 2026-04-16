package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseFactory;

@Service
public class CourseService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final CourseCustomRepository courseCustomRepository;

    @Autowired
    private CourseFactory courseFactory;

    public CourseService(UnitOfWorkService unitOfWorkService, CourseCustomRepository courseCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseCustomRepository = courseCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto getCourseById(Integer aggregateId, UnitOfWork unitOfWork) {
        return courseFactory.createCourseDto(
                (Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto createCourse(CourseDto courseDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Course course = courseFactory.createCourse(aggregateId, courseDto);
        unitOfWorkService.registerChanged(course, unitOfWork);
        return courseFactory.createCourseDto(course);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto getOrCreateCourse(CourseDto courseDto, UnitOfWork unitOfWork) {
        Course course = courseCustomRepository.findCourseIdByName(courseDto.getName())
                .map(id -> (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .orElse(null);
        if (course == null) {
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            course = courseFactory.createCourse(aggregateId, courseDto);
            unitOfWorkService.registerChanged(course, unitOfWork);
        }
        return courseFactory.createCourseDto(course);
    }
}
