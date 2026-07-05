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
class DeleteQuestionCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String QUESTION_TITLE_1 = "What is a sorting algorithm?"
    public static final String QUESTION_CONTENT_1 = "Describe a sorting algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto
    def questionDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        questionDto = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], QUESTION_TITLE_1, QUESTION_CONTENT_1)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteQuestion: fault on deleteQuestionStep compensates the lock acquired by getQuestionStep"() {
        // getQuestionStep has already set QuestionSagaState.IN_DELETE_QUESTION and registered its
        // compensation by the time deleteQuestionStep's injected fault fires, so this exercises the
        // saga's own compensate transition (IN_DELETE_QUESTION -> NOT_IN_SAGA) with no second saga
        // involved.
        when:
        questionFunctionalities.deleteQuestion(questionDto.aggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(questionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: question is still readable, not deleted'
        def reread = questionFunctionalities.getQuestionById(questionDto.aggregateId)
        reread.aggregateId == questionDto.aggregateId
    }
}
