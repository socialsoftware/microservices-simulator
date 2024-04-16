    package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.retry.annotation.Backoff;
    import org.springframework.retry.annotation.Retryable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Isolation;
    import org.springframework.transaction.annotation.Transactional;
    import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
    import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
    import pt.ulisboa.tecnico.socialsoftware.ms.coordination.UnitOfWork;
    import pt.ulisboa.tecnico.socialsoftware.ms.coordination.UnitOfWorkService;
    import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.Course;
    import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseCustomRepository;
    import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;
    import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalCourse;
    import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
    import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseRepository;
    import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

    import java.sql.SQLException;

    @Service
    public class CourseService {
        @Autowired
        private AggregateIdGeneratorService aggregateIdGeneratorService;

        private final UnitOfWorkService unitOfWorkService;

        private final CourseCustomRepository courseRepository;

        public CourseService(UnitOfWorkService unitOfWorkService, CourseCustomRepository courseRepository) {
            this.unitOfWorkService = unitOfWorkService;
            this.courseRepository = courseRepository;
        }

        @Retryable(
                value = { SQLException.class },
                backoff = @Backoff(delay = 5000))
        @Transactional(isolation = Isolation.READ_COMMITTED)
        public CourseDto getCourseById(Integer aggregateId, UnitOfWork unitOfWorkWorkService) {
            return new CourseDto((Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWorkWorkService));
        }

        @Retryable(
                value = { SQLException.class },
                backoff = @Backoff(delay = 5000))
        @Transactional(isolation = Isolation.READ_COMMITTED)
        public CourseExecutionDto getAndOrCreateCourseRemote(CourseExecutionDto courseExecutionDto, UnitOfWork unitOfWork) {
            Course course = getCourseByName(courseExecutionDto.getName(), unitOfWork);
            if (course == null) {
                Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
                course = new CausalCourse(aggregateId, courseExecutionDto);
                unitOfWork.registerChanged(course);
            }
            courseExecutionDto.setCourseAggregateId(course.getAggregateId());
            courseExecutionDto.setName(course.getName());
            courseExecutionDto.setType(course.getType().toString());
            courseExecutionDto.setCourseVersion(course.getVersion());
            return courseExecutionDto;
        }

        private Course getCourseByName(String courseName, UnitOfWork unitOfWork) {
            return courseRepository.findCourseIdByName(courseName)
                    .map(id -> (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                    .orElse(null);
        }
    }
