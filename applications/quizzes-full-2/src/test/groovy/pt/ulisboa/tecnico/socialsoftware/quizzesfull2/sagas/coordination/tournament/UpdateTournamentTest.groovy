package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(UpdateTournamentTest.LocalBeanConfiguration)
class UpdateTournamentTest extends QuizzesFull2SpockTest {

    public static final LocalDateTime UPDATED_START_TIME = DateHandler.now().plusDays(20)
    public static final LocalDateTime UPDATED_END_TIME = DateHandler.now().plusDays(20).plusHours(3)

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

    def "updateTournament: success"() {
        // Spec: plan.md §8 Tournament — UpdateTournament(tournamentAggregateId, startTime, endTime,
        // numberOfQuestions, topicAggregateIds); happy path. The coordinator is void.
        when:
        tournamentFunctionalities.updateTournament(tournamentAggregateId, UPDATED_START_TIME,
                UPDATED_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        then: 'orchestration outcome only — persistence is asserted in T2'
        sagaStateOf(tournamentAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateTournament: getTournamentStep acquires IN_UPDATE_TOURNAMENT semantic lock"() {
        // Spec: plan.md §8 Tournament — saga state IN_UPDATE_TOURNAMENT
        given:
        def uow = unitOfWorkService.createUnitOfWork("UpdateTournament")
        def func = new UpdateTournamentFunctionalitySagas(unitOfWorkService, tournamentAggregateId,
                UPDATED_START_TIME, UPDATED_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS, [], uow,
                commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA → IN_UPDATE_TOURNAMENT'
        sagaStateOf(tournamentAggregateId) == TournamentSagaState.IN_UPDATE_TOURNAMENT

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
