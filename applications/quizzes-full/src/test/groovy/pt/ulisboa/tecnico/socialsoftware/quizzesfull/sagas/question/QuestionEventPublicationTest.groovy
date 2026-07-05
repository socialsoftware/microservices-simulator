package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteQuestionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuestionEventPublicationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String QUESTION_TITLE_1 = "What is a sorting algorithm?"
    public static final String QUESTION_TITLE_UPDATED = "What is a searching algorithm?"
    public static final String QUESTION_CONTENT_1 = "Describe a sorting algorithm."
    public static final String QUESTION_CONTENT_UPDATED = "Describe a searching algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    @Autowired
    EventService eventService

    def "updateQuestion publishes UpdateQuestionEvent with correct payload"() {
        // Spec: plan.md §5 Question — events published by UpdateQuestion
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        def question = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], QUESTION_TITLE_1, QUESTION_CONTENT_1)
        def topics = [new QuestionTopic(topicDto)] as Set

        when:
        questionService.updateQuestion(question.aggregateId, QUESTION_TITLE_UPDATED, QUESTION_CONTENT_UPDATED,
                topics, unitOfWorkService.createUnitOfWork("updateQuestion"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof UpdateQuestionEvent }
        events.size() == 1
        def event = events[0] as UpdateQuestionEvent
        event.publisherAggregateId == question.aggregateId
        event.title == QUESTION_TITLE_UPDATED
        event.content == QUESTION_CONTENT_UPDATED
    }

    def "deleteQuestion publishes DeleteQuestionEvent with correct payload"() {
        // Spec: plan.md §5 Question — events published by DeleteQuestion
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        def question = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], QUESTION_TITLE_1, QUESTION_CONTENT_1)

        when:
        questionService.deleteQuestion(question.aggregateId, unitOfWorkService.createUnitOfWork("deleteQuestion"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteQuestionEvent }
        events.size() == 1
        def event = events[0] as DeleteQuestionEvent
        event.publisherAggregateId == question.aggregateId
        event.courseAggregateId == courseDto.aggregateId
    }

    def "createQuestion does not publish any event"() {
        // Negative case: CreateQuestion has no events-published entry in plan.md §5 Question
        given:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        def topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        def questionCourse = new QuestionCourse(courseDto)
        def topics = [new QuestionTopic(topicDto)] as Set
        def options = [new Option(1, 1, "Option A", true), new Option(2, 2, "Option B", false)] as Set
        def countBefore = eventService.getAllEvents().size()

        when:
        questionService.createQuestion(QUESTION_TITLE_1, QUESTION_CONTENT_1, questionCourse, options, topics,
                unitOfWorkService.createUnitOfWork("createQuestion"))

        then:
        eventService.getAllEvents().size() == countBefore
    }
}
