package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.DeleteTournamentFunctionalitySagas

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteTournamentTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer tournamentId
    LocalDateTime startTime
    LocalDateTime endTime

    def setup() {
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

    def "deleteTournament: success — no participants"() {
        when:
        tournamentFunctionalities.deleteTournament(tournamentId)

        then:
        def dto = tournamentFunctionalities.getTournamentById(tournamentId)
        dto.state == AggregateState.DELETED
    }

    def "deleteTournament: success — cancelled tournament with no participants"() {
        given:
        tournamentFunctionalities.cancelTournament(tournamentId)

        when:
        tournamentFunctionalities.deleteTournament(tournamentId)

        then:
        def dto = tournamentFunctionalities.getTournamentById(tournamentId)
        dto.state == AggregateState.DELETED
    }

    def "deleteTournament: TOURNAMENT_IS_CANCELED violation — cancelled tournament with participants"() {
        given: 'add a participant then cancel'
        def participant = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionId, participant.aggregateId)
        tournamentFunctionalities.addParticipant(tournamentId, executionId, participant.aggregateId)
        tournamentFunctionalities.cancelTournament(tournamentId)

        when: 'try to delete (would clear participants, violating cancelled-frozen invariant)'
        tournamentFunctionalities.deleteTournament(tournamentId)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_IS_CANCELED
    }

    def "deleteTournament: getTournamentStep acquires IN_DELETE_TOURNAMENT semantic lock"() {
        given:
        def uow = unitOfWorkService.createUnitOfWork("deleteTournament")
        def func = new DeleteTournamentFunctionalitySagas(
                unitOfWorkService, tournamentId, uow, commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect:
        sagaStateOf(tournamentId) == TournamentSagaState.IN_DELETE_TOURNAMENT

        when:
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }
}
