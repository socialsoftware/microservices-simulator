package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto

@DataJpaTest
@Transactional
@Import(CreateQuestionTest.LocalBeanConfiguration)
class CreateQuestionTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_COURSE_AGGREGATE_ID = 999999
    public static final Integer NONEXISTENT_TOPIC_AGGREGATE_ID = 999998

    def "createQuestion: success"() {
        // Spec: plan.md §5 Question — CreateQuestion(courseAggregateId, title, content, options, topicAggregateIds)
        given: 'a course with a topic'
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)
        def questionDto = questionDtoFor(courseAggregateId)

        when:
        def result = questionFunctionalities.createQuestion(questionDto, [topicAggregateId])

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.title == QUESTION_TITLE
        result.content == QUESTION_CONTENT
        result.courseAggregateId == courseAggregateId
        result.topics.collect { it.topicAggregateId } == [topicAggregateId]
        result.topics[0].topicName == TOPIC_NAME
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createQuestion: aborts when the course prerequisite does not exist"() {
        // Spec: plan.md §5 Question — cross-aggregate prerequisite (P4a): getCourseStep fetches the
        // course and throws if it does not exist, so no explicit guard is written
        given:
        def questionDto = questionDtoFor(NONEXISTENT_COURSE_AGGREGATE_ID)

        when:
        questionFunctionalities.createQuestion(questionDto, [])

        then:
        thrown(SimulatorException)
    }

    def "createQuestion: aborts when a topic prerequisite does not exist"() {
        // Spec: plan.md §5 Question — cross-aggregate prerequisite (P4a): getTopicsStep fetches each
        // topic and throws if one does not exist, so no explicit guard is written
        given:
        def courseAggregateId = createCourse()
        def questionDto = questionDtoFor(courseAggregateId)

        when:
        questionFunctionalities.createQuestion(questionDto, [NONEXISTENT_TOPIC_AGGREGATE_ID])

        then:
        thrown(SimulatorException)
    }

    // Semantic-lock acquisition: CreateQuestionFunctionalitySagas has no setSemanticLock step — the
    // create step brings the aggregate into existence (sagas.md § Create Functionality Sagas), so
    // there is no prior state to lock, and getCourseStep / getTopicsStep are plain upstream reads.

    private QuestionDto questionDtoFor(Integer courseAggregateId) {
        def questionDto = new QuestionDto()
        questionDto.setCourseAggregateId(courseAggregateId)
        questionDto.setTitle(QUESTION_TITLE)
        questionDto.setContent(QUESTION_CONTENT)
        return questionDto
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
