package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetQuizAnswerForStudentAndQuizTest.LocalBeanConfiguration)
class GetQuizAnswerForStudentAndQuizTest extends QuizzesFull2SpockTest {

    def "getQuizAnswerForStudentAndQuiz: success"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId)
        given: 'a student with a quiz answer session'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        def result = quizAnswerFunctionalities.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == quizAnswerAggregateId
        result.userAggregateId == userAggregateId
        result.quizAggregateId == quizAggregateId
        sagaStateOf(quizAnswerAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
