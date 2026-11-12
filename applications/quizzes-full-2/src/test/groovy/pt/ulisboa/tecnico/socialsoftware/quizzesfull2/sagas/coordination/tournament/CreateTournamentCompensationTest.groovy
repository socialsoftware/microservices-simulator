package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants

// Not mandated by the applicability test — CreateTournament takes no semantic lock — but it is the
// only saga in the application carrying a real registerCompensation, so the undo is worth pinning.
@DataJpaTest
@Transactional
@Import(CreateTournamentCompensationTest.LocalBeanConfiguration)
class CreateTournamentCompensationTest extends QuizzesFull2SpockTest {

    Integer courseAggregateId
    Integer executionAggregateId
    Integer creatorAggregateId

    def setup() {
        loadBehaviorScripts()
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        TOURNAMENT_NUMBER_OF_QUESTIONS.times { index ->
            createQuestion(courseAggregateId, QUESTION_TITLE + " " + index, QUESTION_CONTENT, [])
        }
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "createTournament: fault on createTournamentStep removes the quiz createQuizStep created"() {
        // Spec: plan.md §8 Tournament — CreateTournament also creates the associated Quiz
        when:
        tournamentFunctionalities.createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation removed the orphan quiz: the execution holds no generated quiz'
        def quizzes = quizFunctionalities.getQuizzesForExecution(executionAggregateId)
        quizzes.every { it.title != QuizzesFull2DomainConstants.TOURNAMENT_QUIZ_TITLE }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
