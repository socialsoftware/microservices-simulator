package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.UpdateCourseFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question
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
        result.creationDate != null
        result.optionKeys.size() == 2

        and: 'question is persisted and retrievable'
        def uow = unitOfWorkService.createUnitOfWork("verify")
        Question readBack = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(result.aggregateId, uow)
        readBack.title == QUESTION_TITLE_1
        readBack.content == QUESTION_CONTENT_1
    }

    def "createQuestion: success with no topics"() {
        when:
        QuestionDto result = createQuestion(courseDto.aggregateId, [], QUESTION_TITLE_1, QUESTION_CONTENT_1)

        then:
        result.aggregateId != null
        result.title == QUESTION_TITLE_1
        result.topicIds.isEmpty()
    }

    def "createQuestion: TOPIC_BELONGS_TO_QUESTION_COURSE — topic from different course raises exception"() {
        given: 'a second course with its own topic'
        def courseDto2 = createCourse(COURSE_NAME_2, COURSE_TYPE_TECNICO)
        createExecution(courseDto2.aggregateId, "OTHER-2025", "2025/2026")
        def topicFromOtherCourse = createTopic(courseDto2.aggregateId, TOPIC_NAME_1)

        when: 'trying to assign that topic to a question belonging to course 1'
        createQuestion(courseDto.aggregateId, [topicFromOtherCourse.aggregateId], QUESTION_TITLE_1, QUESTION_CONTENT_1)

        then:
        def ex = thrown(QuizzesFullException)
        ex.errorMessage == QuizzesFullErrorMessage.QUESTION_TOPIC_INVALID_COURSE
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
    def "createQuestion: incrementCourseQuestionCountStep sees forbidden state when course is locked by concurrent updateCourse"() {
        given:
        Set<Option> options = new HashSet<>([
            new Option(1, 1, "Option A", true),
            new Option(2, 2, "Option B", false)
        ])
        def uow1 = unitOfWorkService.createUnitOfWork("createQuestion")
        def func1 = new CreateQuestionFunctionalitySagas(
                unitOfWorkService, QUESTION_TITLE_1, QUESTION_CONTENT_1,
                courseDto.aggregateId, [topicDto.aggregateId], options, uow1, commandGateway)
        func1.executeUntilStep("createQuestionStep", uow1)

        and: 'concurrent updateCourse acquires IN_UPDATE_COURSE on the same course'
        def uow2 = unitOfWorkService.createUnitOfWork("updateCourse")
        def func2 = new UpdateCourseFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, COURSE_NAME_1, COURSE_TYPE_TECNICO, uow2, commandGateway)
        func2.executeUntilStep("getCourseStep", uow2)

        when: 'createQuestion resumes into the forbidden course state'
        func1.resumeWorkflow(uow1)

        then:
        thrown(SimulatorException)
    }
}
