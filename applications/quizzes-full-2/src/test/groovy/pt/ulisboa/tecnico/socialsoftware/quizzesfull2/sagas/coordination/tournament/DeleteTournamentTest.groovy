package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.DeleteTournamentFunctionalitySagas

// No happy-path case: DeleteTournament's own success removes the tournament, so the mandated
// sagaStateOf(...) == NOT_IN_SAGA assertion cannot run. See docs/concepts/testing.md § T4 —
// Functionality Test, "Exception — a functionality whose success makes its own aggregate
// unresolvable"; the lock-acquisition case below, DeleteTournamentCompensationTest and the T2
// delete case together cover the functionality.
@DataJpaTest
@Transactional
@Import(DeleteTournamentTest.LocalBeanConfiguration)
class DeleteTournamentTest extends QuizzesFull2SpockTest {

    @Autowired
    LocalCommandGateway commandGateway

    Integer tournamentAggregateId

    def setup() {
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
    }

    def "deleteTournament: getTournamentStep acquires IN_DELETE_TOURNAMENT semantic lock"() {
        // Spec: plan.md §8 Tournament — saga state IN_DELETE_TOURNAMENT
        given:
        def uow = unitOfWorkService.createUnitOfWork("DeleteTournament")
        def func = new DeleteTournamentFunctionalitySagas(unitOfWorkService, tournamentAggregateId, uow,
                commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_DELETE_TOURNAMENT'
        sagaStateOf(tournamentAggregateId) == TournamentSagaState.IN_DELETE_TOURNAMENT

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
