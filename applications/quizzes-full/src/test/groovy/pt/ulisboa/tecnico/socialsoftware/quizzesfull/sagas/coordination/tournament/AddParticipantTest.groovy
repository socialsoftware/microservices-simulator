package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.CancelTournamentFunctionalitySagas

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_UNIQUE_AS_PARTICIPANT

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class AddParticipantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    Integer courseId
    Integer creatorId
    Integer participantId
    Integer executionId
    Integer topicId
    Integer tournamentId
    LocalDateTime startTime
    LocalDateTime endTime

    def setup() {
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

        startTime = LocalDateTime.now().plusDays(1)
        endTime = LocalDateTime.now().plusDays(2)

        def tournament = createTournament(executionId, creatorId, [topicId], 1, startTime, endTime)
        tournamentId = tournament.aggregateId
    }

    def "addParticipant: success"() {
        when:
        tournamentFunctionalities.addParticipant(tournamentId, executionId, participantId)

        then:
        def dto = tournamentFunctionalities.getTournamentById(tournamentId)
        dto.participantIds.contains(participantId)
    }

    def "addParticipant: PARTICIPANT_COURSE_EXECUTION violation — participant not enrolled"() {
        given: 'a user not enrolled in the execution'
        def notEnrolled = createUser("Not Enrolled", "notenrolled", STUDENT_ROLE)

        when:
        tournamentFunctionalities.addParticipant(tournamentId, executionId, notEnrolled.aggregateId)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == COURSE_EXECUTION_STUDENT_NOT_FOUND
    }

    def "addParticipant: TOURNAMENT_UNIQUE_AS_PARTICIPANT violation — participant already enrolled"() {
        given:
        tournamentFunctionalities.addParticipant(tournamentId, executionId, participantId)

        when:
        tournamentFunctionalities.addParticipant(tournamentId, executionId, participantId)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_UNIQUE_AS_PARTICIPANT
    }

    def "addParticipant: getTournamentStep acquires IN_ADD_PARTICIPANT semantic lock"() {
        given: 'workflow paused after getTournamentStep has acquired IN_ADD_PARTICIPANT lock'
        def uow = unitOfWorkService.createUnitOfWork("addParticipant")
        def func = new AddParticipantFunctionalitySagas(
                unitOfWorkService, tournamentId, executionId, participantId, uow, commandGateway)
        func.executeUntilStep("getTournamentStep", uow)

        expect: 'tournament saga state is IN_ADD_PARTICIPANT'
        sagaStateOf(tournamentId) == TournamentSagaState.IN_ADD_PARTICIPANT

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }

    def "addParticipant: TOURNAMENT_IS_CANCELED violation — tournament already cancelled"() {
        given: 'tournament is cancelled'
        tournamentFunctionalities.cancelTournament(tournamentId)

        when:
        tournamentFunctionalities.addParticipant(tournamentId, executionId, participantId)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED
    }
}
