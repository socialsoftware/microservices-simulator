    package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service;

    import org.springframework.beans.factory.annotation.Autowired;
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

        @Autowired
        private CourseFactory courseFactory;

        public CourseService(UnitOfWorkService unitOfWorkService, CourseCustomRepository courseRepository) {
            this.unitOfWorkService = unitOfWorkService;
            this.courseRepository = courseRepository;
        }

        @Retryable(
                retryFor = { SQLException.class },
                maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public CourseDto getCourseById(Integer aggregateId, UnitOfWork unitOfWorkWorkService) {
            return courseFactory.createCourseDto((Course) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWorkWorkService));
        }

        @Retryable(
                retryFor = { SQLException.class },
                maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
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

        private Course getCourseByName(String courseName, UnitOfWork unitOfWork) {
            return courseRepository.findCourseIdByName(courseName)
                    .map(id -> (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                    .orElse(null);
        }
    }
