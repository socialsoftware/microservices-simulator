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
@Import(GetTournamentByIdTest.LocalBeanConfiguration)
class GetTournamentByIdTest extends QuizzesFull2SpockTest {

    def "getTournamentById: success"() {
        // Spec: plan.md §8 Tournament — GetTournamentById(tournamentAggregateId)
        given: 'an execution with an enrolled creator and one tournament'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)

        when:
        def result = tournamentFunctionalities.getTournamentById(tournamentAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId == tournamentAggregateId
        result.executionAggregateId == executionAggregateId
        result.creatorAggregateId == creatorAggregateId
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
