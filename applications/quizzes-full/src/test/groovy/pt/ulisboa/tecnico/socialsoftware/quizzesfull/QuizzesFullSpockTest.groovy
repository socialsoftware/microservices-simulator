package pt.ulisboa.tecnico.socialsoftware.quizzesfull

import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.functionalities.CourseFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.functionalities.TopicFunctionalities

class QuizzesFullSpockTest extends SpockTest {

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    public static final String COURSE_NAME_1 = "Software Engineering"
    public static final String COURSE_NAME_2 = "Distributed Systems"
    public static final String COURSE_TYPE_TECNICO = "TECNICO"
    public static final String COURSE_TYPE_EXTERNAL = "EXTERNAL"

    public static final String USER_NAME_1 = "John Doe"
    public static final String USER_NAME_2 = "Jane Doe"
    public static final String USER_USERNAME_1 = "johndoe"
    public static final String STUDENT_ROLE = "STUDENT"

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService

    @Autowired(required = false)
    protected CourseFunctionalities courseFunctionalities

    @Autowired(required = false)
    protected CourseService courseService

    @Autowired(required = false)
    protected UserFunctionalities userFunctionalities

    @Autowired(required = false)
    protected UserService userService

    @Autowired(required = false)
    protected TopicFunctionalities topicFunctionalities

    @Autowired(required = false)
    protected ExecutionFunctionalities executionFunctionalities

    @Autowired(required = false)
    protected ExecutionService executionService

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

    CourseDto createCourse(String name, String type) {
        return courseFunctionalities.createCourse(name, type)
    }

    UserDto createUser(String name, String username, String role) {
        return userFunctionalities.createUser(new UserDto(null, name, username, role, false))
    }

    TopicDto createTopic(Integer courseId, String name) {
        TopicDto dto = new TopicDto()
        dto.name = name
        return topicFunctionalities.createTopic(courseId, dto)
    }

    ExecutionDto createExecution(Integer courseId, String acronym, String academicTerm) {
        return executionFunctionalities.createExecution(acronym, academicTerm, courseId)
    }
}
