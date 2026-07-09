package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament
// P1 intra-invariant violations are NOT tested here — see TournamentIntraInvariantTest.

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class SolveQuizTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer creatorId
    Integer participantId
    Integer executionId
    Integer topicId
    Integer tournamentId
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

        endTime = LocalDateTime.now().plusDays(2)
        tournamentId = createTournament(executionId, creatorId, [topicId], 1,
                LocalDateTime.now().plusDays(1), endTime).aggregateId
    }

    private Integer prepareTournamentReadyForSolveQuiz(Integer userId) {
        def tournament = createStartedTournament(executionId, creatorId, [topicId], 1)
        addParticipantEnrolledBeforeStart(tournament.aggregateId, userId, tournament.startTime)
        return tournament.aggregateId
    }

    def "solveQuiz: success — links participant quiz answer to tournament"() {
        given: 'participant is enrolled and has a quiz answer on a started tournament'
        def readyTournamentId = prepareTournamentReadyForSolveQuiz(participantId)
        def tournamentDto = tournamentFunctionalities.getTournamentById(readyTournamentId)
        def quizAnswer = createQuizAnswer(tournamentDto.quizAggregateId, participantId)

        when:
        tournamentFunctionalities.solveQuiz(readyTournamentId, participantId)

        then:
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def updated = unitOfWorkService.aggregateLoadAndRegisterRead(readyTournamentId, checkUow) as Tournament
        def participant = updated.participants.find { it.participantAggregateId == participantId }
        participant != null
        participant.quizAnswer.quizAnswerAggregateId == quizAnswer.aggregateId
    }

    // "QUIZ_ANSWER_NOT_FOUND — no quiz answer for participant" case removed (session 3.7): it is a
    // straight duplicate of QuizAnswerServiceTest's "getQuizAnswerByQuizIdAndStudentId: not found"
    // case — SolveQuizFunctionalitySagas.getQuizAnswerStep resolves through the same
    // GetQuizAnswerByQuizIdAndStudentIdCommand -> QuizAnswerService.getQuizAnswerByQuizIdAndStudentId
    // path, so this was cross-aggregate duplicate coverage, not missing coverage (same category as
    // the 3.5 GetQuestionsByCourseExecutionIdTest note).
}
