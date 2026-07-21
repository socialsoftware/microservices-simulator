package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuestionCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String QUESTION_TITLE_1 = "What is a sorting algorithm?"
    public static final String QUESTION_CONTENT_1 = "Describe a sorting algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def executionDto
    def topicDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "createQuestion: fault on getTopicsStep compensates the lock acquired by getCourseStep"() {
        // getCourseStep has already set CourseSagaState.READ_COURSE and registered its
        // compensation by the time getTopicsStep's injected fault fires. The Question doesn't
        // exist yet at this point in the saga, so the released lock is on Course.
        given:
        Set<Option> options = new HashSet<>([new Option(1, 1, "Option A", true)])

        when:
        questionFunctionalities.createQuestion(QUESTION_TITLE_1, QUESTION_CONTENT_1,
                courseDto.aggregateId, [topicDto.aggregateId], options)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the Course semantic lock back to NOT_IN_SAGA'
        sagaStateOf(courseDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: no question was created'
        questionFunctionalities.getQuestionsByCourseExecutionId(executionDto.aggregateId).isEmpty()
    }
}
