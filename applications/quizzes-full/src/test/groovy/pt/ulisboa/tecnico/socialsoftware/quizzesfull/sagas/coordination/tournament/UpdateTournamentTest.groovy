package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas

// P1 intra-invariant violations are NOT tested here — see TournamentIntraInvariantTest.

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateTournamentTest extends QuizzesFullSpockTest {

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

    def "updateTournament: success — orchestration outcome only"() {
        // Persistence is asserted in TournamentServiceTest.
        given:
        def newStart = LocalDateTime.now().plusDays(3)
        def newEnd = LocalDateTime.now().plusDays(5)

        when:
        tournamentFunctionalities.updateTournament(tournamentId, newStart, newEnd, [])

        then:
        noExceptionThrown()
        sagaStateOf(tournamentId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateTournament: getTournamentStep acquires IN_UPDATE_TOURNAMENT semantic lock"() {
        given:
        def newStart = LocalDateTime.now().plusDays(3)
        def newEnd = LocalDateTime.now().plusDays(5)
        def uow = unitOfWorkService.createUnitOfWork("updateTournament")
        def func = new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, tournamentId, newStart, newEnd, [], uow, commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect:
        sagaStateOf(tournamentId) == TournamentSagaState.IN_UPDATE_TOURNAMENT

        when:
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }
}
