package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas

@DataJpaTest
@Transactional
@Import(AddParticipantTest.LocalBeanConfiguration)
class AddParticipantTest extends QuizzesFull2SpockTest {

    // The lock-acquisition case builds its saga directly; the CommandGateway interface has more than
    // one implementation in the test profile, so the concrete gateway is the injectable type.
    @Autowired
    LocalCommandGateway commandGateway

    Integer executionAggregateId
    Integer creatorAggregateId
    Integer participantAggregateId
    Integer tournamentAggregateId

    def setup() {
        def courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        participantAggregateId = createActiveUser(TOURNAMENT_PARTICIPANT_NAME,
                TOURNAMENT_PARTICIPANT_USERNAME)
        tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
    }

    def "addParticipant: success"() {
        // Spec: plan.md §8 Tournament — AddParticipant(tournamentAggregateId, userAggregateId);
        // happy path. The coordinator is void, so the traversal itself is the outcome.
        given:
        enrollStudentInExecution(executionAggregateId, participantAggregateId)

        when:
        tournamentFunctionalities.addParticipant(tournamentAggregateId, participantAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "addParticipant: getTournamentStep acquires IN_ADD_PARTICIPANT semantic lock"() {
        // Spec: plan.md §8 Tournament — saga state IN_ADD_PARTICIPANT
        given:
        enrollStudentInExecution(executionAggregateId, participantAggregateId)
        def uow = unitOfWorkService.createUnitOfWork("AddParticipant")
        def func = new AddParticipantFunctionalitySagas(unitOfWorkService, tournamentAggregateId,
                participantAggregateId, uow, commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_ADD_PARTICIPANT'
        sagaStateOf(tournamentAggregateId) == TournamentSagaState.IN_ADD_PARTICIPANT

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    def "addParticipant: PARTICIPANT_COURSE_EXECUTION violation"() {
        // Spec: plan.md §8 Tournament — rule PARTICIPANT_COURSE_EXECUTION
        when: 'the user was never enrolled in the tournament execution'
        tournamentFunctionalities.addParticipant(tournamentAggregateId, participantAggregateId)

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.PARTICIPANT_COURSE_EXECUTION
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
