package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private final CourseCustomRepository courseCustomRepository;
    private final CourseFactory courseFactory;
    private final UnitOfWorkService unitOfWorkService;

    public CourseService(CourseCustomRepository courseCustomRepository,
                         CourseFactory courseFactory,
                         UnitOfWorkService unitOfWorkService) {
        this.courseCustomRepository = courseCustomRepository;
        this.courseFactory = courseFactory;
        this.unitOfWorkService = unitOfWorkService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseDto getCourseById(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return courseFactory.createCourseDto(
                (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<CourseDto> getCourses(UnitOfWork unitOfWork) {
        return courseCustomRepository.findAllCourseIds().stream()
                .map(courseAggregateId -> courseFactory.createCourseDto(
                        (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }
}
