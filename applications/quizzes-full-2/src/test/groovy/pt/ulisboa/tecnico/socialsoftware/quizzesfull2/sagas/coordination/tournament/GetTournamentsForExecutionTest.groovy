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
@Import(GetTournamentsForExecutionTest.LocalBeanConfiguration)
class GetTournamentsForExecutionTest extends QuizzesFull2SpockTest {

    public static final Integer SECOND_NUMBER_OF_QUESTIONS = 8

    def "getTournamentsForExecution: success"() {
        // Spec: plan.md §8 Tournament — GetTournamentsForExecution(executionAggregateId)
        given: 'an execution with two tournaments'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def firstAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        def secondAggregateId = createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, SECOND_NUMBER_OF_QUESTIONS)

        when:
        def result = tournamentFunctionalities.getTournamentsForExecution(executionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.size() == 2
        result.collect { it.aggregateId } as Set == [firstAggregateId, secondAggregateId] as Set
        result.find { it.aggregateId == secondAggregateId }.numberOfQuestions == SECOND_NUMBER_OF_QUESTIONS
        sagaStateOf(firstAggregateId) == GenericSagaState.NOT_IN_SAGA
        sagaStateOf(secondAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
