package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteQuestionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.OptionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Question
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto

@DataJpaTest
@Transactional
@Import(QuestionServiceTest.LocalBeanConfiguration)
class QuestionServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_QUESTION_TITLE = "Graph traversal"
    public static final String SECOND_QUESTION_CONTENT = "What is the complexity of breadth-first search?"
    public static final String UPDATED_QUESTION_TITLE = "Merge sort complexity"
    public static final String UPDATED_QUESTION_CONTENT = "State the worst-case complexity of merge sort."

    @Autowired
    EventService eventService

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

    def "createQuestion: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §5 Question — CreateQuestion(courseAggregateId, title, content, options, topicAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)
        def questionDto = new QuestionDto()
        questionDto.setCourseAggregateId(courseAggregateId)
        questionDto.setTitle(QUESTION_TITLE)
        questionDto.setContent(QUESTION_CONTENT)
        questionDto.setOptions([new OptionDto(1, 10, "O(n log n)", true),
                                new OptionDto(2, 11, "O(n^2)", false)])
        def topics = [new QuestionTopicDto(topicAggregateId, TOPIC_NAME, QUESTION_TOPIC_VERSION,
                courseAggregateId)]

        when:
        def result = questionService.createQuestion(questionDto, topics,
                unitOfWorkService.createUnitOfWork("createQuestion"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        result.aggregateId != null
        flushAndClear()
        def readBack = questionService.getQuestionById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.title == QUESTION_TITLE
        readBack.content == QUESTION_CONTENT
        readBack.courseAggregateId == courseAggregateId
        readBack.creationDate != null
        readBack.options.size() == 2
        readBack.options.find { it.optionKey == 10 }.content == "O(n log n)"
        readBack.options.find { it.optionKey == 10 }.isCorrect()
        !readBack.options.find { it.optionKey == 11 }.isCorrect()
        readBack.topics.size() == 1
        readBack.topics[0].topicAggregateId == topicAggregateId
        readBack.topics[0].topicName == TOPIC_NAME
        readBack.topics[0].courseAggregateId == courseAggregateId
    }

    def "updateQuestion: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §5 Question — UpdateQuestion(questionAggregateId, title, content, topicAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def topicAggregateId = createTopic(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def topics = [new QuestionTopicDto(topicAggregateId, TOPIC_NAME, QUESTION_TOPIC_VERSION,
                courseAggregateId)]

        when:
        questionService.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, topics,
                unitOfWorkService.createUnitOfWork("updateQuestion"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = questionService.getQuestionById(questionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.title == UPDATED_QUESTION_TITLE
        readBack.content == UPDATED_QUESTION_CONTENT
        readBack.courseAggregateId == courseAggregateId
        readBack.topics.size() == 1
        readBack.topics[0].topicAggregateId == topicAggregateId
        readBack.topics[0].topicName == TOPIC_NAME
    }

    def "updateQuestion: replaces the previous topic set"() {
        // Spec: plan.md §5 Question — UpdateQuestion(questionAggregateId, title, content, topicAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def firstTopicAggregateId = createTopic(courseAggregateId)
        def secondTopicAggregateId = createTopic(courseAggregateId, "Concurrency")
        def questionAggregateId = createQuestion(courseAggregateId)
        questionService.updateQuestion(questionAggregateId, QUESTION_TITLE, QUESTION_CONTENT,
                [new QuestionTopicDto(firstTopicAggregateId, TOPIC_NAME, QUESTION_TOPIC_VERSION,
                        courseAggregateId)],
                unitOfWorkService.createUnitOfWork("updateQuestion"))

        when:
        questionService.updateQuestion(questionAggregateId, QUESTION_TITLE, QUESTION_CONTENT,
                [new QuestionTopicDto(secondTopicAggregateId, "Concurrency", QUESTION_TOPIC_VERSION,
                        courseAggregateId)],
                unitOfWorkService.createUnitOfWork("updateQuestion"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = questionService.getQuestionById(questionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.topics.collect { it.topicAggregateId } == [secondTopicAggregateId]
    }

    def "updateQuestion: unknown aggregate id is not found"() {
        // Spec: plan.md §5 Question — UpdateQuestion(questionAggregateId, ...); Path A (aggregateLoadAndRegisterRead)
        when:
        questionService.updateQuestion(NONEXISTENT_AGGREGATE_ID, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [],
                unitOfWorkService.createUnitOfWork("updateQuestion"))

        then:
        thrown(SimulatorException)
    }

    def "deleteQuestion: soft-deletes the question, keeping its fields"() {
        // Spec: plan.md §5 Question — DeleteQuestion(questionAggregateId) soft-deletes the question
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        questionService.deleteQuestion(questionAggregateId,
                unitOfWorkService.createUnitOfWork("deleteQuestion"))

        then: 'read back off a cleared persistence context, through the deleted-version load path'
        flushAndClear()
        def readBack = (Question) unitOfWorkService.aggregateDeletedLoad(questionAggregateId)
        readBack.state == Aggregate.AggregateState.DELETED
        readBack.title == QUESTION_TITLE
        readBack.content == QUESTION_CONTENT
        readBack.courseAggregateId == courseAggregateId
    }

    def "deleteQuestion: the deleted question is no longer visible to the non-deleted load path"() {
        // Spec: plan.md §5 Question — DeleteQuestion(questionAggregateId) soft-deletes the question
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)
        questionService.deleteQuestion(questionAggregateId,
                unitOfWorkService.createUnitOfWork("deleteQuestion"))

        when:
        flushAndClear()
        questionService.getQuestionById(questionAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "deleteQuestion: unknown aggregate id is not found"() {
        // Spec: plan.md §5 Question — DeleteQuestion(questionAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        questionService.deleteQuestion(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteQuestion"))

        then:
        thrown(SimulatorException)
    }

    def "updateQuestion publishes UpdateQuestionEvent with correct payload"() {
        // Spec: plan.md §5 Question — events published: UpdateQuestionEvent; payload questionAggregateId, title, content
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        questionService.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [],
                unitOfWorkService.createUnitOfWork("updateQuestion"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof UpdateQuestionEvent }
        events.size() == 1
        def event = events[0] as UpdateQuestionEvent
        event.publisherAggregateId == questionAggregateId
        event.questionAggregateId == questionAggregateId
        event.title == UPDATED_QUESTION_TITLE
        event.content == UPDATED_QUESTION_CONTENT
    }

    def "deleteQuestion publishes DeleteQuestionEvent with correct payload"() {
        // Spec: plan.md §5 Question — events published: DeleteQuestionEvent; payload questionAggregateId, courseAggregateId
        given:
        def courseAggregateId = createCourse()
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        questionService.deleteQuestion(questionAggregateId,
                unitOfWorkService.createUnitOfWork("deleteQuestion"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteQuestionEvent }
        events.size() == 1
        def event = events[0] as DeleteQuestionEvent
        event.publisherAggregateId == questionAggregateId
        event.questionAggregateId == questionAggregateId
        event.courseAggregateId == courseAggregateId
    }

    def "createQuestion publishes no event"() {
        // Spec: plan.md §5 Question — only UpdateQuestionEvent and DeleteQuestionEvent are published
        given:
        def courseAggregateId = createCourse()
        def countBefore = eventService.getAllEvents().size()
        def questionDto = new QuestionDto()
        questionDto.setCourseAggregateId(courseAggregateId)
        questionDto.setTitle(SECOND_QUESTION_TITLE)
        questionDto.setContent(SECOND_QUESTION_CONTENT)

        when:
        questionService.createQuestion(questionDto, [],
                unitOfWorkService.createUnitOfWork("createQuestion"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
