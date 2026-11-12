package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(UpdateTournamentCompensationTest.LocalBeanConfiguration)
class UpdateTournamentCompensationTest extends QuizzesFull2SpockTest {

    public static final LocalDateTime UPDATED_START_TIME = testNow().plusDays(20)
    public static final LocalDateTime UPDATED_END_TIME = testNow().plusDays(20).plusHours(3)

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

    def "updateTournament: fault on updateTournamentStep compensates the lock acquired by getTournamentStep"() {
        // Spec: plan.md §8 Tournament — UpdateTournament(tournamentAggregateId, ...);
        // saga state IN_UPDATE_TOURNAMENT
        when:
        tournamentFunctionalities.updateTournament(tournamentAggregateId, UPDATED_START_TIME,
                UPDATED_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the update never ran: the tournament still carries its pre-saga window'
        def reread = tournamentFunctionalities.getTournamentById(tournamentAggregateId)
        reread.startTime == TOURNAMENT_START_TIME
        reread.endTime == TOURNAMENT_END_TIME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
