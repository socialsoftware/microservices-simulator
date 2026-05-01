package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.SagaTopic

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create topic"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setVersion(1L)
        def topicCourse = new TopicCourse(courseDto)

        when:
        def topic = new SagaTopic(1, "Topic Name", topicCourse)

        then:
        topic.name == "Topic Name"
        topic.topicCourse.courseAggregateId == 100
        topic.topicCourse.courseVersion == 1L
    }
}
