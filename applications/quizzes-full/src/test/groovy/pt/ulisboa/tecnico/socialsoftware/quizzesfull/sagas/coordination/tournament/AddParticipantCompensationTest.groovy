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
class AddParticipantCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer creatorId
    Integer participantId
    Integer executionId
    Integer topicId
    Integer tournamentId

    def setup() {
        loadBehaviorScripts()

        def course = createCourse("Software Engineering", "TECNICO")
        courseId = course.aggregateId

        def creator = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        creatorId = creator.aggregateId

        def participant = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        participantId = participant.aggregateId

        def execution = createExecution(courseId, "SE2024", "1st Semester 2024")
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, creatorId)
        executionFunctionalities.enrollStudentInExecution(executionId, participantId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        createQuestion(courseId, [topicId], "Q1", "Content 1")

        def tournament = createTournament(executionId, creatorId, [topicId], 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2))
        tournamentId = tournament.aggregateId
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "addParticipant: fault on addParticipantStep compensates the lock acquired by getTournamentStep"() {
        // getTournamentStep depends on getStudentStep <- getUserStep (3 levels deep, not a root
        // step), so under the old fixed two-pass ExecutionPlan this compensation could never be
        // genuinely exercised - the pass would fault addParticipantStep before getTournamentStep's
        // real body (and its IN_ADD_PARTICIPANT lock acquisition) ever ran. The topological
        // worklist fix gates addParticipantStep's fault check on the full real dependency chain's
        // completion at arbitrary depth, so the lock is genuinely held and then genuinely released
        // by compensation.
        when:
        tournamentFunctionalities.addParticipant(tournamentId, executionId, participantId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the Tournament semantic lock back to NOT_IN_SAGA'
        sagaStateOf(tournamentId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: participant was never added'
        !tournamentFunctionalities.getTournamentById(tournamentId).participantIds.contains(participantId)
    }
}
