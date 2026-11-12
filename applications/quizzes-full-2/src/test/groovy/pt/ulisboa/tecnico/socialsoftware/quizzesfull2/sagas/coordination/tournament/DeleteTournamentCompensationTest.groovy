package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(DeleteTournamentCompensationTest.LocalBeanConfiguration)
class DeleteTournamentCompensationTest extends QuizzesFull2SpockTest {

    Integer tournamentAggregateId

    def setup() {
        loadBehaviorScripts()
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteTournament: fault on deleteTournamentStep compensates the lock acquired by getTournamentStep"() {
        // Spec: plan.md §8 Tournament — DeleteTournament(tournamentAggregateId);
        // saga state IN_DELETE_TOURNAMENT
        when:
        tournamentFunctionalities.deleteTournament(tournamentAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the delete never ran: the tournament still resolves'
        def reread = tournamentFunctionalities.getTournamentById(tournamentAggregateId)
        reread.aggregateId == tournamentAggregateId
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
