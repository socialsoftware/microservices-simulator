package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_FINAL_AFTER_START
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_START_BEFORE_END_TIME

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

    def "updateTournament: success — update times"() {
        given:
        def newStart = LocalDateTime.now().plusDays(3)
        def newEnd = LocalDateTime.now().plusDays(5)

        when:
        tournamentFunctionalities.updateTournament(tournamentId, newStart, newEnd, [])

        then:
        def dto = tournamentFunctionalities.getTournamentById(tournamentId)
        dto.startTime == newStart
        dto.endTime == newEnd
    }

    def "updateTournament: TOURNAMENT_START_BEFORE_END_TIME violation"() {
        given:
        def badStart = LocalDateTime.now().plusDays(5)
        def badEnd = LocalDateTime.now().plusDays(3)

        when:
        tournamentFunctionalities.updateTournament(tournamentId, badStart, badEnd, [])

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_START_BEFORE_END_TIME
    }

    def "updateTournament: TOURNAMENT_IS_CANCELED violation — tournament already cancelled"() {
        given:
        tournamentFunctionalities.cancelTournament(tournamentId)

        when:
        tournamentFunctionalities.updateTournament(tournamentId,
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(5), [])

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_IS_CANCELED
    }

    def "updateTournament: TOURNAMENT_FINAL_AFTER_START violation — tournament already started"() {
        given: 'a tournament whose start time is already in the past'
        def pastTournamentId = createTournament(executionId, userId, [topicId], 1,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1)).aggregateId

        when: 'update is attempted after start time with new times'
        tournamentFunctionalities.updateTournament(pastTournamentId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), [])

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_FINAL_AFTER_START
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
    def "updateTournament: updateQuizStep sees forbidden state when quiz is locked by concurrent updateQuiz"() {
        given:
        def tournament = tournamentFunctionalities.getTournamentById(tournamentId)
        def quizId = tournament.quizAggregateId
        def newStart = LocalDateTime.now().plusDays(3)
        def newEnd = LocalDateTime.now().plusDays(5)
        def uow = unitOfWorkService.createUnitOfWork("updateTournament")
        def func = new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, tournamentId, newStart, newEnd, [], uow, commandGateway)
        func.executeUntilStep("updateTournamentStep", uow)

        and: 'concurrent updateQuiz acquires IN_UPDATE_QUIZ on the same quiz'
        def uowQuiz = unitOfWorkService.createUnitOfWork("updateQuiz")
        def updateQuizFunc = new UpdateQuizFunctionalitySagas(
                unitOfWorkService, quizId,
                LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7), [], uowQuiz, commandGateway)
        updateQuizFunc.executeUntilStep("getQuizStep", uowQuiz)

        when: 'updateTournament resumes into the forbidden quiz state'
        func.resumeWorkflow(uow)

        then:
        thrown(SimulatorException)
    }
}
