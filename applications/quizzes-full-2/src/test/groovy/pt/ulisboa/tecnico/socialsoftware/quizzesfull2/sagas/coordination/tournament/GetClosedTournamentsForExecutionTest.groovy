package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(GetClosedTournamentsForExecutionTest.LocalBeanConfiguration)
class GetClosedTournamentsForExecutionTest extends QuizzesFull2SpockTest {

    public static final LocalDateTime PAST_START_TIME = DateHandler.now().minusDays(2)
    public static final LocalDateTime PAST_END_TIME = DateHandler.now().minusDays(1)

    def "getClosedTournamentsForExecution: success"() {
        // Spec: plan.md §8 Tournament — GetClosedTournamentsForExecution(executionAggregateId)
        given: 'an execution with one still-open and one already-ended tournament'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        createTournament(executionAggregateId, creatorAggregateId)
        def closedAggregateId = createTournament(executionAggregateId, creatorAggregateId,
                PAST_START_TIME, PAST_END_TIME)

        when:
        def result = tournamentFunctionalities.getClosedTournamentsForExecution(executionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.collect { it.aggregateId } == [closedAggregateId]
        result[0].endTime == PAST_END_TIME
        sagaStateOf(closedAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
