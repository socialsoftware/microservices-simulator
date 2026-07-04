package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.topic

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TopicServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String TOPIC_NAME_1 = "Algorithms"
    public static final String TOPIC_NAME_UPDATED = "Advanced Algorithms"

    def "createTopic: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §3 Topic — CreateTopic postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def input = new TopicDto()
        input.name = TOPIC_NAME_1

        when:
        def dto = topicService.createTopic(input, new TopicCourse(courseDto),
                unitOfWorkService.createUnitOfWork("createTopic"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = topicService.getTopicById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == TOPIC_NAME_1
        readBack.courseId == courseDto.aggregateId
    }

    def "getTopicById: not found throws SimulatorException"() {
        // Spec: plan.md §3 Topic — GetTopicById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        topicService.getTopicById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getTopicById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateTopic: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §3 Topic — UpdateTopic postconditions
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def existing = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        def update = new TopicDto()
        update.aggregateId = existing.aggregateId
        update.name = TOPIC_NAME_UPDATED

        when:
        topicService.updateTopic(update, unitOfWorkService.createUnitOfWork("updateTopic"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = topicService.getTopicById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == TOPIC_NAME_UPDATED
    }

    def "deleteTopic: removes topic, not found via fresh UnitOfWork"() {
        // Spec: plan.md §3 Topic — DeleteTopic postconditions (soft-delete)
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def existing = createTopic(courseDto.aggregateId, TOPIC_NAME_1)

        when:
        topicService.deleteTopic(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("deleteTopic"))

        and: 'verify topic is no longer retrievable'
        topicService.getTopicById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }
}
