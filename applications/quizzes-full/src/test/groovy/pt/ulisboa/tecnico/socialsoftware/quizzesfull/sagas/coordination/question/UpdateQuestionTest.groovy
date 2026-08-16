package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.question

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.states.QuestionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateQuestionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String QUESTION_TITLE_ORIGINAL = "What is a sorting algorithm?"
    public static final String QUESTION_TITLE_UPDATED  = "What is a searching algorithm?"
    public static final String QUESTION_CONTENT_ORIGINAL = "Describe a sorting algorithm."
    public static final String QUESTION_CONTENT_UPDATED  = "Describe a searching algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto1
    def questionDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto1 = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        questionDto = createQuestion(courseDto.aggregateId, [topicDto1.aggregateId], QUESTION_TITLE_ORIGINAL, QUESTION_CONTENT_ORIGINAL)
    }

    def "updateQuestion: success"() {
        // Spec: plan.md §5 Question — UpdateQuestion; orchestration outcome only, persistence in QuestionServiceTest.
        when:
        questionFunctionalities.updateQuestion(questionDto.aggregateId, QUESTION_TITLE_UPDATED, QUESTION_CONTENT_UPDATED, [topicDto1.aggregateId])

        then:
        noExceptionThrown()
        sagaStateOf(questionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateQuestion: getQuestionStep acquires IN_UPDATE_QUESTION semantic lock"() {
        given: 'updateQuestion workflow pauses after getQuestionStep has acquired IN_UPDATE_QUESTION lock'
        def uow1 = unitOfWorkService.createUnitOfWork("updateQuestion")
        def func1 = new UpdateQuestionFunctionalitySagas(
                unitOfWorkService, questionDto.aggregateId,
                QUESTION_TITLE_UPDATED, QUESTION_CONTENT_UPDATED,
                [topicDto1.aggregateId], uow1, commandGateway)
        func1.executeUntilStep("getQuestionStep", uow1)

        expect: 'question saga state is IN_UPDATE_QUESTION'
        sagaStateOf(questionDto.aggregateId) == QuestionSagaState.IN_UPDATE_QUESTION

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
        sagaStateOf(questionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }
}
