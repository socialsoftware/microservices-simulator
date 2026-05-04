package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.CreateQuestionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuestionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String QUESTION_TITLE_1 = "What is a sorting algorithm?"
    public static final String QUESTION_CONTENT_1 = "Describe a sorting algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        // execution needed so course's executionCount > 0 (CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT invariant)
        createExecution(courseDto.aggregateId, "SE-2025", "2025/2026")
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
    }

    def "createQuestion: success"() {
        when:
        QuestionDto result = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], QUESTION_TITLE_1, QUESTION_CONTENT_1)

        then:
        result.aggregateId != null
        result.title == QUESTION_TITLE_1
        result.content == QUESTION_CONTENT_1
        result.courseAggregateId == courseDto.aggregateId
        result.topicIds.contains(topicDto.aggregateId)
    }

    def "createQuestion: success with no topics"() {
        when:
        QuestionDto result = createQuestion(courseDto.aggregateId, [], QUESTION_TITLE_1, QUESTION_CONTENT_1)

        then:
        result.aggregateId != null
        result.title == QUESTION_TITLE_1
        result.topicIds.isEmpty()
    }

    def "createQuestion: getCourseStep acquires READ_COURSE semantic lock before question is created"() {
        given: 'a createQuestion workflow pauses after getCourseStep has acquired READ_COURSE lock'
        Set<Option> options = new HashSet<>([new Option(1, 1, "Option A", true)])
        def uow1 = unitOfWorkService.createUnitOfWork("createQuestion")
        def func1 = new CreateQuestionFunctionalitySagas(
                unitOfWorkService, QUESTION_TITLE_1, QUESTION_CONTENT_1,
                courseDto.aggregateId, [topicDto.aggregateId], options, uow1, commandGateway)
        func1.executeUntilStep("getCourseStep", uow1)

        expect: 'course saga state is READ_COURSE'
        sagaStateOf(courseDto.aggregateId) == CourseSagaState.READ_COURSE

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()

        and: 'question was created'
        func1.getCreatedQuestionDto().aggregateId != null
        func1.getCreatedQuestionDto().title == QUESTION_TITLE_1
        func1.getCreatedQuestionDto().courseAggregateId == courseDto.aggregateId
    }
}
