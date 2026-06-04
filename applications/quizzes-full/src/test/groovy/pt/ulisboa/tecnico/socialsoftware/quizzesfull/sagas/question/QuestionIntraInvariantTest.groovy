package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.SagaQuestion
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUESTION_TOPIC_INVALID_COURSE

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuestionIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create question"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setVersion(1L)
        def questionCourse = new QuestionCourse(courseDto)
        def option = new Option(1, 42, "Option A", true)
        def options = [option] as Set

        when:
        def question = new SagaQuestion(1, "Sample Question", "What is 2+2?", questionCourse, options, [] as Set)

        then:
        question.title == "Sample Question"
        question.content == "What is 2+2?"
        question.creationDate != null
        question.questionCourse.courseAggregateId == 100
        question.questionCourse.courseVersion == 1L
        question.options.size() == 1
        question.topics.isEmpty()
    }

    def "QUESTION_TOPIC_INVALID_COURSE: topic whose courseId differs from question courseId violates invariant"() {
        given: "a question belonging to course 100"
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setVersion(1L)
        def questionCourse = new QuestionCourse(courseDto)
        def option = new Option(1, 42, "Option A", true)
        def question = new SagaQuestion(1, "Sample Question", "What is 2+2?", questionCourse, [option] as Set, [] as Set)

        and: "a cached topic snapshot from a different course (courseId = 999)"
        def topicDto = new TopicDto()
        topicDto.setAggregateId(50)
        topicDto.setVersion(1L)
        topicDto.setName("Algorithms")
        topicDto.setCourseId(999)
        def mismatchedTopic = new QuestionTopic(topicDto)
        question.addTopic(mismatchedTopic)

        when:
        question.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == QUESTION_TOPIC_INVALID_COURSE
    }
}
