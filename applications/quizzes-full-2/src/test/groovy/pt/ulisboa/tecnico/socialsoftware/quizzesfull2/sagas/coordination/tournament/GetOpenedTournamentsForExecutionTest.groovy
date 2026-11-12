package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(GetOpenedTournamentsForExecutionTest.LocalBeanConfiguration)
class GetOpenedTournamentsForExecutionTest extends QuizzesFull2SpockTest {

    def "getOpenedTournamentsForExecution: success"() {
        // Spec: plan.md §8 Tournament — GetOpenedTournamentsForExecution(executionAggregateId)
        given: 'an execution with one still-open and one already-ended tournament'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def openAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        createClosedTournament(executionAggregateId, creatorAggregateId)

        when:
        def result = tournamentFunctionalities.getOpenedTournamentsForExecution(executionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.collect { it.aggregateId } == [openAggregateId]
        result[0].endTime == TOURNAMENT_END_TIME
        sagaStateOf(openAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
