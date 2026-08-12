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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.CancelTournamentFunctionalitySagas

@DataJpaTest
@Transactional
@Import(CancelTournamentTest.LocalBeanConfiguration)
class CancelTournamentTest extends QuizzesFull2SpockTest {

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

    def "cancelTournament: success"() {
        // Spec: plan.md §8 Tournament — CancelTournament(tournamentAggregateId); happy path.
        // The coordinator is void.
        when:
        tournamentFunctionalities.cancelTournament(tournamentAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "cancelTournament: getTournamentStep acquires IN_CANCEL_TOURNAMENT semantic lock"() {
        // Spec: plan.md §8 Tournament — saga state IN_CANCEL_TOURNAMENT
        given:
        def uow = unitOfWorkService.createUnitOfWork("CancelTournament")
        def func = new CancelTournamentFunctionalitySagas(unitOfWorkService, tournamentAggregateId, uow,
                commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_CANCEL_TOURNAMENT'
        sagaStateOf(tournamentAggregateId) == TournamentSagaState.IN_CANCEL_TOURNAMENT

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
