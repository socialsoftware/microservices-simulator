package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateQuestionCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String QUESTION_TITLE_ORIGINAL = "What is a sorting algorithm?"
    public static final String QUESTION_TITLE_UPDATED = "What is a searching algorithm?"
    public static final String QUESTION_CONTENT_ORIGINAL = "Describe a sorting algorithm."
    public static final String QUESTION_CONTENT_UPDATED = "Describe a searching algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto
    def questionDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        questionDto = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], QUESTION_TITLE_ORIGINAL, QUESTION_CONTENT_ORIGINAL)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateQuestion: fault on getTopicsStep compensates the lock acquired by getQuestionStep"() {
        // getQuestionStep has already set QuestionSagaState.IN_UPDATE_QUESTION and registered its
        // compensation by the time getTopicsStep's injected fault fires, so this exercises the
        // saga's own compensate transition (IN_UPDATE_QUESTION -> NOT_IN_SAGA) with no second saga
        // involved.
        when:
        questionFunctionalities.updateQuestion(questionDto.aggregateId, QUESTION_TITLE_UPDATED,
                QUESTION_CONTENT_UPDATED, [topicDto.aggregateId])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(questionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: question still has its original title and content'
        def reread = questionFunctionalities.getQuestionById(questionDto.aggregateId)
        reread.title == QUESTION_TITLE_ORIGINAL
        reread.content == QUESTION_CONTENT_ORIGINAL
    }
}
