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
class CreateQuizAnswerCompensationTest extends QuizzesFullSpockTest {

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

    def "createQuizAnswer: fault on getUserStep compensates the lock acquired by getQuizStep"() {
        // getQuizStep is a root (no-dependency) step, so it genuinely runs and registers its
        // compensation before getUserStep's injected fault fires.
        when:
        createQuizAnswer(quizId, userId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the Quiz semantic lock back to NOT_IN_SAGA'
        sagaStateOf(quizId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: no quiz answer was created'
        quizAnswerRepository.findAll().isEmpty()
    }
}
