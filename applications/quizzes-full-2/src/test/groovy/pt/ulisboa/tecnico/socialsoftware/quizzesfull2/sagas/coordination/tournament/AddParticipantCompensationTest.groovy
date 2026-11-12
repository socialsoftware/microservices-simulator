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
@Import(AddParticipantCompensationTest.LocalBeanConfiguration)
class AddParticipantCompensationTest extends QuizzesFull2SpockTest {

    Integer participantAggregateId
    Integer tournamentAggregateId

    def setup() {
        loadBehaviorScripts()
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        participantAggregateId = createActiveUser(TOURNAMENT_PARTICIPANT_NAME,
                TOURNAMENT_PARTICIPANT_USERNAME)
        enrollStudentInExecution(executionAggregateId, participantAggregateId)
        tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "addParticipant: fault on addParticipantStep compensates the lock acquired by getTournamentStep"() {
        // Spec: plan.md §8 Tournament — AddParticipant(tournamentAggregateId, userAggregateId);
        // saga state IN_ADD_PARTICIPANT
        when:
        tournamentFunctionalities.addParticipant(tournamentAggregateId, participantAggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the enrolment never ran: the tournament still has no participant'
        def reread = tournamentFunctionalities.getTournamentById(tournamentAggregateId)
        reread.participants.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
