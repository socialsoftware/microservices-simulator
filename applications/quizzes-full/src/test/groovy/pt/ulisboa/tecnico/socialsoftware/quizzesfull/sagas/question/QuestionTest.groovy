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

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuestionTest extends QuizzesFullSpockTest {

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

    def "TOPIC_BELONGS_TO_QUESTION_COURSE: topic from different course raises exception"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setVersion(1L)
        def questionCourse = new QuestionCourse(courseDto)

        def topicDto = new TopicDto()
        topicDto.setAggregateId(200)
        topicDto.setVersion(1L)
        topicDto.setName("Topic A")
        topicDto.setCourseId(999)  // different course
        def topic = new QuestionTopic(topicDto)

        def question = new SagaQuestion(1, "Q", "Content", questionCourse, [] as Set, [topic] as Set)

        when:
        question.verifyInvariants()

        then:
        thrown(QuizzesFullException)
    }
}
