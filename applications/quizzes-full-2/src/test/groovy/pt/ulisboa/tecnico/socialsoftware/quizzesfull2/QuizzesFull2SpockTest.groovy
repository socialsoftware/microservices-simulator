package pt.ulisboa.tecnico.socialsoftware.quizzesfull2

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService

// Domain imports (DTOs, functionalities, services) are added here as aggregates are implemented in Phase 2.
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.functionalities.CourseFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.SagaExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.service.UserService

import java.time.LocalDateTime

class QuizzesFull2SpockTest extends SpockTest {

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    // Domain constants are added here as aggregates are implemented in Phase 2.
    public static final Integer COURSE_AGGREGATE_ID = 1
    public static final Integer COURSE_AGGREGATE_ID_2 = 6
    public static final String COURSE_NAME = "Software Engineering"
    public static final CourseType COURSE_TYPE = CourseType.TECNICO

    public static final Integer USER_AGGREGATE_ID = 2
    public static final String USER_NAME = "Alice Smith"
    public static final String USER_USERNAME = "alice"
    public static final Role USER_ROLE = Role.STUDENT

    public static final Integer TOPIC_AGGREGATE_ID = 3
    public static final String TOPIC_NAME = "Algorithms"

    public static final Integer EXECUTION_AGGREGATE_ID = 4
    public static final String EXECUTION_ACRONYM = "SE-01"
    public static final String EXECUTION_ACADEMIC_TERM = "2025/2026"
    public static final LocalDateTime EXECUTION_END_DATE = LocalDateTime.of(2026, 7, 31, 23, 59)

    public static final Integer EXECUTION_STUDENT_USER_AGGREGATE_ID = 20
    public static final Integer EXECUTION_STUDENT_USER_AGGREGATE_ID_2 = 21
    public static final String EXECUTION_STUDENT_USER_NAME = "Bob Jones"
    public static final String EXECUTION_STUDENT_USER_USERNAME = "bob"
    public static final Long EXECUTION_STUDENT_USER_VERSION = 1L

    public static final Integer QUESTION_AGGREGATE_ID = 5
    public static final String QUESTION_TITLE = "Sorting complexity"
    public static final String QUESTION_CONTENT = "What is the worst-case complexity of merge sort?"
    public static final LocalDateTime QUESTION_CREATION_DATE = LocalDateTime.of(2025, 9, 1, 10, 0)

    public static final Integer QUESTION_TOPIC_AGGREGATE_ID = 30
    public static final String QUESTION_TOPIC_NAME = "Algorithms"
    public static final Long QUESTION_TOPIC_VERSION = 1L

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService
    @Autowired(required = false)
    protected AggregateIdGeneratorService aggregateIdGeneratorService
    @PersistenceContext
    protected EntityManager entityManager

    // Domain @Autowired fields are added here as aggregates are implemented in Phase 2.
    @Autowired(required = false)
    protected CourseService courseService
    @Autowired(required = false)
    protected CourseFunctionalities courseFunctionalities
    @Autowired(required = false)
    protected UserService userService
    @Autowired(required = false)
    protected UserFunctionalities userFunctionalities
    @Autowired(required = false)
    protected TopicService topicService
    @Autowired(required = false)
    protected TopicFunctionalities topicFunctionalities
    @Autowired(required = false)
    protected ExecutionService executionService
    @Autowired(required = false)
    protected ExecutionFunctionalities executionFunctionalities

    def loadBehaviorScripts() {
        def mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)
        def scriptDir = "groovy/" + this.class.simpleName
        impairmentService.LoadDir(mavenBaseDir, scriptDir)
    }

    SagaState sagaStateOf(Integer aggregateId) {
        def uow = unitOfWorkService.createUnitOfWork("TEST")
        def agg = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow)
        return agg.getSagaState()
    }

    protected <T> T loadForCheck(Integer aggregateId, Class<T> type) {
        def uow = unitOfWorkService.createUnitOfWork("check")
        return type.cast(unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow))
    }

    // Forces the next read to go through Hibernate's instantiation path. Under @DataJpaTest every
    // UnitOfWork in a test shares one persistence context, so a "fresh UnitOfWork" read-back returns
    // the managed write instance and never exercises the load path - final fields set reflectively
    // on load, @Convert converters and lazy associations all go unproven without this.
    protected void flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    // Domain create* helpers are added below as aggregates are implemented in Phase 2.

    Integer createCourse(String name = COURSE_NAME, CourseType type = COURSE_TYPE) {
        def courseDto = new CourseDto()
        courseDto.setName(name)
        courseDto.setType(type)
        return courseFunctionalities.createCourse(courseDto).aggregateId
    }

    Integer createUser(String name = USER_NAME, String username = USER_USERNAME, Role role = USER_ROLE) {
        def userDto = new UserDto()
        userDto.setName(name)
        userDto.setUsername(username)
        userDto.setRole(role)
        return userFunctionalities.createUser(userDto).aggregateId
    }

    Integer createTopic(Integer courseAggregateId, String name = TOPIC_NAME) {
        def topicDto = new TopicDto()
        topicDto.setName(name)
        topicDto.setCourseAggregateId(courseAggregateId)
        return topicFunctionalities.createTopic(topicDto).aggregateId
    }

    Integer createExecution(Integer courseAggregateId, String acronym = EXECUTION_ACRONYM,
                            String academicTerm = EXECUTION_ACADEMIC_TERM,
                            LocalDateTime endDate = EXECUTION_END_DATE) {
        def executionDto = new ExecutionDto()
        executionDto.setCourseAggregateId(courseAggregateId)
        executionDto.setAcronym(acronym)
        executionDto.setAcademicTerm(academicTerm)
        executionDto.setEndDate(endDate)
        return executionFunctionalities.createExecution(executionDto).aggregateId
    }

    void enrollStudentInExecution(Integer executionAggregateId, Integer userAggregateId) {
        executionFunctionalities.enrollStudentInExecution(executionAggregateId, userAggregateId)
    }

    // The enroll functionality fetches the user and rejects an inactive one (INACTIVE_USER), so a
    // student fixture is a created user plus an activation.
    Integer createActiveUser(String name = USER_NAME, String username = USER_USERNAME, Role role = USER_ROLE) {
        def userAggregateId = createUser(name, username, role)
        userFunctionalities.activateUser(userAggregateId)
        return userAggregateId
    }
}
