package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType

@DataJpaTest
@Transactional
@Import(QuestionServiceTest.LocalBeanConfiguration)
class QuestionServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_QUESTION_TITLE = "Graph traversal"
    public static final String SECOND_QUESTION_CONTENT = "What is the complexity of breadth-first search?"

    def "getQuestionById: reads back the persisted question through a fresh UnitOfWork"() {
        // Spec: plan.md §5 Question — fields title, content, creationDate, courseAggregateId
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        flushAndClear()
        def result = questionService.getQuestionById(questionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == questionAggregateId
        result.title == QUESTION_TITLE
        result.content == QUESTION_CONTENT
        result.courseAggregateId == courseAggregateId
        result.creationDate != null
        result.options.isEmpty()
        result.topics.isEmpty()
        result.version != null
    }

    def "getQuestionById: unknown aggregate id is not found"() {
        // Spec: plan.md §5 Question — Path A (aggregateLoadAndRegisterRead)
        when:
        questionService.getQuestionById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getQuestionsByCourse: returns every question of the course with its own state"() {
        // Spec: plan.md §5 Question — GetQuestionsByCourse(courseAggregateId)
        given:
        def courseAggregateId = createCourse()
        def firstAggregateId = createQuestion(courseAggregateId)
        def secondAggregateId = createQuestion(courseAggregateId, SECOND_QUESTION_TITLE,
                SECOND_QUESTION_CONTENT)

        when:
        flushAndClear()
        def result = questionService.getQuestionsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.title == QUESTION_TITLE
        first.content == QUESTION_CONTENT
        first.courseAggregateId == courseAggregateId
        def second = result.find { it.aggregateId == secondAggregateId }
        second.title == SECOND_QUESTION_TITLE
        second.content == SECOND_QUESTION_CONTENT
        second.courseAggregateId == courseAggregateId
    }

    def "getQuestionsByCourse: excludes questions belonging to another course"() {
        // Spec: plan.md §5 Question — GetQuestionsByCourse(courseAggregateId) filters on courseAggregateId
        given:
        def courseAggregateId = createCourse()
        def otherCourseAggregateId = createCourse("Distributed Systems", CourseType.EXTERNAL)
        def questionAggregateId = createQuestion(courseAggregateId)
        createQuestion(otherCourseAggregateId, SECOND_QUESTION_TITLE, SECOND_QUESTION_CONTENT)

        when:
        flushAndClear()
        def result = questionService.getQuestionsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [questionAggregateId]
    }

    def "getQuestionsByCourse: returns an empty list when the course has no question"() {
        // Spec: plan.md §5 Question — GetQuestionsByCourse(courseAggregateId)
        given:
        def courseAggregateId = createCourse()

        when:
        def result = questionService.getQuestionsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
