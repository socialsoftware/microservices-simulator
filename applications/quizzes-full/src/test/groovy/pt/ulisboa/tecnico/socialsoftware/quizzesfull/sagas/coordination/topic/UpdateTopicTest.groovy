package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.states.TopicSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String TOPIC_NAME_ORIGINAL = "Algorithms"
    public static final String TOPIC_NAME_UPDATED  = "Advanced Algorithms"

    def courseDto
    def topicDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_ORIGINAL)
    }

    def "updateTopic: success — name is persisted"() {
        given:
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = TOPIC_NAME_UPDATED

        when:
        topicFunctionalities.updateTopic(update)

        then:
        def uow = unitOfWorkService.createUnitOfWork("verify")
        Topic fetched = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.aggregateId, uow)
        fetched.name == TOPIC_NAME_UPDATED
    }

    def "updateTopic: null name throws exception"() {
        given:
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = null

        when:
        topicFunctionalities.updateTopic(update)

        then:
        thrown(QuizzesFullException)
    }

    def "updateTopic: getTopicStep acquires READ_TOPIC semantic lock before update completes"() {
        given: 'an update payload'
        TopicDto update = new TopicDto()
        update.aggregateId = topicDto.aggregateId
        update.name = TOPIC_NAME_UPDATED

        and: 'updateTopic workflow pauses after getTopicStep has acquired READ_TOPIC lock'
        def uow1 = unitOfWorkService.createUnitOfWork("updateTopic")
        def func1 = new UpdateTopicFunctionalitySagas(
                unitOfWorkService, update, uow1, commandGateway)
        func1.executeUntilStep("getTopicStep", uow1)

        expect: 'topic saga state is READ_TOPIC'
        sagaStateOf(topicDto.aggregateId) == TopicSagaState.READ_TOPIC

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()

        and: 'name was updated'
        def uow2 = unitOfWorkService.createUnitOfWork("verify")
        Topic fetched = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.aggregateId, uow2)
        fetched.name == TOPIC_NAME_UPDATED
    }
}
