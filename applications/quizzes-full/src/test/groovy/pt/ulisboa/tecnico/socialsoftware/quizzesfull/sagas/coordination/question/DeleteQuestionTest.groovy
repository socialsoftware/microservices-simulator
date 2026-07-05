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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.DeleteQuestionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteQuestionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String QUESTION_TITLE_1 = "What is a sorting algorithm?"
    public static final String QUESTION_CONTENT_1 = "Describe a sorting algorithm."
    public static final String TOPIC_NAME_1 = "Algorithms"

    def courseDto
    def topicDto
    def questionDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        topicDto = createTopic(courseDto.aggregateId, TOPIC_NAME_1)
        questionDto = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], QUESTION_TITLE_1, QUESTION_CONTENT_1)
    }

    def "deleteQuestion: success"() {
        // Spec: plan.md §5 Question — DeleteQuestion; orchestration outcome only, persistence in QuestionServiceTest.
        when:
        questionFunctionalities.deleteQuestion(questionDto.aggregateId)

        then:
        noExceptionThrown()
    }

    def "deleteQuestion: getQuestionStep acquires IN_DELETE_QUESTION semantic lock before deletion completes"() {
        given: 'deleteQuestion workflow pauses after getQuestionStep has acquired IN_DELETE_QUESTION lock'
        def uow1 = unitOfWorkService.createUnitOfWork("deleteQuestion")
        def func1 = new DeleteQuestionFunctionalitySagas(
                unitOfWorkService, questionDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getQuestionStep", uow1)

        expect: 'question saga state is IN_DELETE_QUESTION'
        sagaStateOf(questionDto.aggregateId) == QuestionSagaState.IN_DELETE_QUESTION

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
    }
}
