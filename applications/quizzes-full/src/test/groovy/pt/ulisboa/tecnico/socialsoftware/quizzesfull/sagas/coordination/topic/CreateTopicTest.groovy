package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.topic

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.UpdateCourseFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.CreateTopicFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateTopicTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String TOPIC_NAME_1 = "Algorithms"
    public static final String TOPIC_NAME_2 = "Data Structures"

    def courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "createTopic: success"() {
        // Spec: Topic.{name,courseId} = input; SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.3 Topic / createTopic.
        when:
        TopicDto result = createTopic(courseDto.aggregateId, TOPIC_NAME_1)

        then:
        result.aggregateId != null
        result.name == TOPIC_NAME_1
        result.courseId == courseDto.aggregateId
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createTopic: success with second topic on same course"() {
        given:
        createTopic(courseDto.aggregateId, TOPIC_NAME_1)

        // Spec: Topic.{name,courseId} = input; SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.3 Topic / createTopic.
        when:
        TopicDto result = createTopic(courseDto.aggregateId, TOPIC_NAME_2)

        then:
        result.aggregateId != null
        result.name == TOPIC_NAME_2
        result.courseId == courseDto.aggregateId
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createTopic: createTopicStep completes when concurrent updateCourse holds course lock"() {
        given: 'a topic DTO to create'
        TopicDto topicDto = new TopicDto()
        topicDto.name = TOPIC_NAME_1

        and: 'createTopic workflow pauses after getCourseStep has acquired READ_COURSE lock'
        def uow1 = unitOfWorkService.createUnitOfWork("createTopic")
        def func1 = new CreateTopicFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, topicDto, uow1, commandGateway)
        func1.executeUntilStep("getCourseStep", uow1)

        expect: 'course saga state is READ_COURSE'
        sagaStateOf(courseDto.aggregateId) == CourseSagaState.READ_COURSE

        and: 'concurrent updateCourse acquires IN_UPDATE_COURSE on the same course'
        def uow2 = unitOfWorkService.createUnitOfWork("updateCourse")
        def func2 = new UpdateCourseFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, COURSE_NAME_1, COURSE_TYPE_TECNICO, uow2, commandGateway)
        func2.executeUntilStep("getCourseStep", uow2)

        when: 'createTopic resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()

        and: 'topic was created'
        func1.getCreatedTopicDto().aggregateId != null
        func1.getCreatedTopicDto().name == TOPIC_NAME_1
        func1.getCreatedTopicDto().courseId == courseDto.aggregateId
    }
}
