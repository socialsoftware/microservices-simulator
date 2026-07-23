package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerRepository

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuizAnswerUserLockCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuizAnswerRepository quizAnswerRepository

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId

    def setup() {
        loadBehaviorScripts()

        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        courseId = course.aggregateId

        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        userId = user.aggregateId

        def execution = createExecution(courseId, ACRONYM_1, ACADEMIC_TERM_1)
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, userId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        def question = createQuestion(courseId, [topicId], "Q1", "Content")
        questionId = question.aggregateId

        def quiz = createQuiz(executionId, [questionId])
        quizId = quiz.aggregateId
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "createQuizAnswer: fault on createQuizAnswerStep compensates the lock acquired by getUserStep"() {
        // getUserStep depends on getQuizStep (not a root step), so under the old fixed two-pass
        // ExecutionPlan this compensation could never be genuinely exercised - the pass would
        // fault createQuizAnswerStep before getUserStep's real body (and its own READ_USER lock
        // acquisition) ever ran. The topological worklist fix gates createQuizAnswerStep's fault
        // check on getUserStep's real completion, so the User lock is genuinely held and then
        // genuinely released by compensation. (The sibling getQuizStep/Quiz-lock scenario is
        // covered separately by CreateQuizAnswerCompensationTest.)
        when:
        createQuizAnswer(quizId, userId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the User semantic lock back to NOT_IN_SAGA'
        sagaStateOf(userId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: no quiz answer was created'
        quizAnswerRepository.findAll().isEmpty()
    }
}
