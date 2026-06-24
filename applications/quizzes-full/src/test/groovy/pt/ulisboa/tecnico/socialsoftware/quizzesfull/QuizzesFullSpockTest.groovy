package pt.ulisboa.tecnico.socialsoftware.quizzesfull

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

// Functionalities and DTOs will be imported here as they are scaffolded in Phase 2

import java.time.LocalDateTime

class QuizzesFullSpockTest extends SpockTest {
    public static final String ANONYMOUS = "ANONYMOUS"

    public static final LocalDateTime TIME_1 = DateHandler.now().plusMinutes(5)
    public static final LocalDateTime TIME_2 = DateHandler.now().plusMinutes(25)
    public static final LocalDateTime TIME_3 = DateHandler.now().plusHours(1).plusMinutes(5)
    public static final LocalDateTime TIME_4 = DateHandler.now().plusHours(1).plusMinutes(25)

    public static final String COURSE_NAME = 'Software Engineering'
    public static final String COURSE_TYPE = 'TECNICO'
    public static final String COURSE_EXECUTION_ACRONYM = 'ES2425'
    public static final String COURSE_EXECUTION_ACADEMIC_TERM = '2024/2025'

    public static final String USER_NAME_1 = "USER_NAME_1"
    public static final String USER_NAME_2 = "USER_NAME_2"
    public static final String USER_NAME_3 = "USER_NAME_3"

    public static final String USER_USERNAME_1 = "USER_USERNAME_1"
    public static final String USER_USERNAME_2 = "USER_USERNAME_2"
    public static final String USER_USERNAME_3 = "USER_USERNAME_3"

    public static final String STUDENT_ROLE = 'STUDENT'
    public static final String TEACHER_ROLE = 'TEACHER'
    public static final String ADMIN_ROLE = 'ADMIN'

    public static final String TOPIC_NAME_1 = "TOPIC_NAME_1"
    public static final String TOPIC_NAME_2 = "TOPIC_NAME_2"
    public static final String TOPIC_NAME_3 = "TOPIC_NAME_3"

    public static final String TITLE_1 = 'Title One'
    public static final String TITLE_2 = 'Title Two'
    public static final String CONTENT_1 = 'Content One'
    public static final String CONTENT_2 = 'Content Two'
    public static final String OPTION_1 = "Option One"
    public static final String OPTION_2 = "Option Two"

    public static final String DISCUSSION_MESSAGE_1 = "Discussion message one"
    public static final String REPLY_MESSAGE_1 = "Reply message one"
    public static final String REVIEW_COMMENT_1 = "Review comment one"

    public static final Integer NUMBER_OF_QUESTIONS = 3
    public static final String PASSWORD_1 = "password123"

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService

    // @Autowired fields for functionalities will be added as aggregates are scaffolded

    def loadBehaviorScripts() {
        def mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)
        def scriptDir = "groovy/" + this.class.simpleName
        impairmentService.LoadDir(mavenBaseDir, scriptDir)
    }

    // Generic: get saga state for any saga aggregate by ID
    SagaState sagaStateOf(Integer aggregateId) {
        def uow = unitOfWorkService.createUnitOfWork("TEST")
        def agg = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow)
        return agg.getSagaState()
    }
}
