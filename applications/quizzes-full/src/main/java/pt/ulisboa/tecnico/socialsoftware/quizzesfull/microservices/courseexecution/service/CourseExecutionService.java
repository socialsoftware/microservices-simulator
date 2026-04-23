package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionFactory;

@Service
public class CourseExecutionService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final CourseExecutionCustomRepository courseExecutionCustomRepository;

    @Autowired
    private CourseExecutionFactory courseExecutionFactory;

    public CourseExecutionService(UnitOfWorkService unitOfWorkService,
            CourseExecutionCustomRepository courseExecutionCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseExecutionCustomRepository = courseExecutionCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto getCourseExecutionById(Integer aggregateId, UnitOfWork unitOfWork) {
        return courseExecutionFactory.createCourseExecutionDto(
                (CourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CourseExecutionDto createCourseExecution(CourseExecutionDto dto, UnitOfWork unitOfWork) {
        CourseExecutionCourse courseExecutionCourse = new CourseExecutionCourse(dto);
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        CourseExecution courseExecution = courseExecutionFactory.createCourseExecution(aggregateId, dto, courseExecutionCourse);
        unitOfWorkService.registerChanged(courseExecution, unitOfWork);
        return courseExecutionFactory.createCourseExecutionDto(courseExecution);
    }
}
