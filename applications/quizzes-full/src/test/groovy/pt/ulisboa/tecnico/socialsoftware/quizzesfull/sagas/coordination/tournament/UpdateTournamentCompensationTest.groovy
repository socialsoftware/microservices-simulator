package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTournamentCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer tournamentId
    LocalDateTime startTime
    LocalDateTime endTime

    def setup() {
        loadBehaviorScripts()

        def course = createCourse("Software Engineering", "TECNICO")
        courseId = course.aggregateId

        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        userId = user.aggregateId

        def execution = createExecution(courseId, "SE2024", "1st Semester 2024")
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, userId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        createQuestion(courseId, [topicId], "Q1", "Content 1")

        startTime = LocalDateTime.now().plusDays(1)
        endTime = LocalDateTime.now().plusDays(2)

        def tournament = createTournament(executionId, userId, [topicId], 1, startTime, endTime)
        tournamentId = tournament.aggregateId
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateTournament: fault on getTopicsStep compensates the lock acquired by getTournamentStep"() {
        // getTournamentStep is a root (no-dependency) step, so it genuinely runs and registers its
        // compensation before getTopicsStep's injected fault fires.
        given:
        def newStart = LocalDateTime.now().plusDays(3)
        def newEnd = LocalDateTime.now().plusDays(5)

        when:
        tournamentFunctionalities.updateTournament(tournamentId, newStart, newEnd, [])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tournamentId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = tournamentFunctionalities.getTournamentById(tournamentId)
        reread.startTime == startTime
        reread.endTime == endTime
    }
}
