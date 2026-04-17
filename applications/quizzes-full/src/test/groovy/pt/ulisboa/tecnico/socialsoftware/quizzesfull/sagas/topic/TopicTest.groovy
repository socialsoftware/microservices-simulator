package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.service.TopicService

@DataJpaTest
@Import(BeanConfigurationSagas)
class TopicTest extends QuizzesFullSpockTest {

    @Autowired
    TopicService topicService

    def "create a topic successfully"() {
        given: "a topic DTO"
        def topicDto = new TopicDto()
        topicDto.setName(TOPIC_NAME_1)
        topicDto.setCourseId(1)

        when: "the topic is created"
        def uow = unitOfWorkService.createUnitOfWork("createTopic")
        def result = topicService.createTopic(topicDto, uow)
        unitOfWorkService.commit(uow)

        then: "the returned DTO has the correct fields"
        result.getName() == TOPIC_NAME_1
        result.getCourseId() == 1
        result.getAggregateId() != null
    }

    def "create a topic without a courseId"() {
        given: "a topic DTO with no courseId"
        def topicDto = new TopicDto()
        topicDto.setName(TOPIC_NAME_2)
        topicDto.setCourseId(null)

        when: "the topic is created"
        def uow = unitOfWorkService.createUnitOfWork("createTopic")
        def result = topicService.createTopic(topicDto, uow)
        unitOfWorkService.commit(uow)

        then: "creation succeeds with null courseId (snapshot not yet populated)"
        result.getName() == TOPIC_NAME_2
        result.getCourseId() == null
        result.getAggregateId() != null
    }
}
