package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.states.TopicSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteTopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
    }

    def "deleteTopic: success"() {
        when:
        topicFunctionalities.deleteTopic(topicDto.aggregateId)

        and: 'verify topic is no longer retrievable'
        def uow = unitOfWorkService.createUnitOfWork("verify")
        unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.aggregateId, uow)

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "deleteTopic: deleteTopicStep fails when topic is deleted by concurrent deleteTopic"() {
        given: 'deleteTopic workflow pauses after getTopicStep has acquired READ_TOPIC lock'
        def uow1 = unitOfWorkService.createUnitOfWork("deleteTopic")
        def func1 = new DeleteTopicFunctionalitySagas(
                unitOfWorkService, topicDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getTopicStep", uow1)

        expect: 'topic saga state is READ_TOPIC'
        sagaStateOf(topicDto.aggregateId) == TopicSagaState.READ_TOPIC

        and: 'concurrent deleteTopic acquires READ_TOPIC and completes deletion'
        def uow2 = unitOfWorkService.createUnitOfWork("deleteTopic2")
        def func2 = new DeleteTopicFunctionalitySagas(
                unitOfWorkService, topicDto.aggregateId, uow2, commandGateway)
        func2.executeUntilStep("getTopicStep", uow2)
        func2.resumeWorkflow(uow2)

        when: 'first deleteTopic resumes into already-deleted topic'
        func1.resumeWorkflow(uow1)

        then:
        thrown(SimulatorException)
    }
}
