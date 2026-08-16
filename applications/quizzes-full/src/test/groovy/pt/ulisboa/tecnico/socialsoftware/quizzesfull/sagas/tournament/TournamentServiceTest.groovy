package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_AFTER_END
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_TOPIC_COURSE_MISMATCH

import java.time.LocalDateTime

// T2 — Service: TournamentService method contracts. Tournament publishes no events (plan.md §8:
// "Events published: —"), so there are no event-publication cases in this file.
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TournamentServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId
    LocalDateTime startTime
    LocalDateTime endTime

    def setup() {
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        courseId = course.aggregateId

        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        userId = user.aggregateId

        def execution = createExecution(courseId, ACRONYM_1, ACADEMIC_TERM_1)
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, userId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        def question = createQuestion(courseId, [topicId], "Q1", "Content")
        questionId = question.aggregateId

        def quiz = createQuiz(executionId, [questionId])
        quizId = quiz.aggregateId

        startTime = LocalDateTime.now().plusDays(1)
        endTime = LocalDateTime.now().plusDays(2)
    }

    private buildCreateTournamentArgs() {
        def executionDto = executionService.getExecutionById(executionId, unitOfWorkService.createUnitOfWork("check"))
        def userDto = userService.getUserById(userId, unitOfWorkService.createUnitOfWork("check"))
        def topicDto = topicService.getTopicById(topicId, unitOfWorkService.createUnitOfWork("check"))
        def quizDto = quizService.getQuizById(quizId, unitOfWorkService.createUnitOfWork("check"))
        return [executionDto, userDto, topicDto, quizDto]
    }

    def "createTournament: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — CreateTournament postconditions
        given:
        def (executionDto, userDto, topicDto, quizDto) = buildCreateTournamentArgs()

        when:
        def dto = tournamentService.createTournament(
                executionDto.aggregateId, executionDto.version, executionDto.courseId,
                userDto.aggregateId, userDto.name, userDto.username, userDto.version,
                [topicDto],
                quizDto.aggregateId, quizDto.version,
                startTime, endTime, 1,
                unitOfWorkService.createUnitOfWork("createTournament"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(dto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.executionAggregateId == executionId
        readBack.creatorAggregateId == userId
        readBack.numberOfQuestions == 1
        readBack.cancelled == false
    }

    def "createTournament: TOURNAMENT_TOPIC_COURSE_MISMATCH violation — topic from different course"() {
        // Spec: plan.md §8 Tournament — rule TOPIC_COURSE_EXECUTION (P3 service guard,
        // TournamentService.createTournament)
        given:
        def (executionDto, userDto, _, quizDto) = buildCreateTournamentArgs()
        def otherCourse = createCourse("Other Course", COURSE_TYPE_TECNICO)
        def otherTopic = createTopic(otherCourse.aggregateId, "Other Topic")
        def otherTopicDto = topicService.getTopicById(otherTopic.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        when:
        tournamentService.createTournament(
                executionDto.aggregateId, executionDto.version, executionDto.courseId,
                userDto.aggregateId, userDto.name, userDto.username, userDto.version,
                [otherTopicDto],
                quizDto.aggregateId, quizDto.version,
                startTime, endTime, 1,
                unitOfWorkService.createUnitOfWork("createTournament"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_TOPIC_COURSE_MISMATCH
    }

    def "getTournamentById: not found throws SimulatorException"() {
        // Spec: plan.md §8 Tournament — GetTournamentById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        tournamentService.getTournamentById(NONEXISTENT_AGGREGATE_ID, unitOfWorkService.createUnitOfWork("getTournamentById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "addParticipant: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — AddParticipant postconditions
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)
        def participant = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def participantDto = userService.getUserById(participant.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        when:
        tournamentService.addParticipant(
                tournamentDto.aggregateId,
                participantDto.aggregateId, participantDto.name, participantDto.username, participantDto.version,
                unitOfWorkService.createUnitOfWork("addParticipant"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.participantIds.contains(participant.aggregateId)
    }

    def "updateTournament: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — UpdateTournament postconditions
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)
        def newStart = LocalDateTime.now().plusDays(3)
        def newEnd = LocalDateTime.now().plusDays(5)

        when:
        tournamentService.updateTournament(tournamentDto.aggregateId, newStart, newEnd, [],
                unitOfWorkService.createUnitOfWork("updateTournament"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.startTime == newStart
        readBack.endTime == newEnd
    }

    def "cancelTournament: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — CancelTournament postconditions
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.cancelTournament(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("cancelTournament"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.cancelled == true
    }

    def "cancelTournament: TOURNAMENT_IS_CANCELED violation — already cancelled"() {
        // Spec: plan.md §8 Tournament — inline service guard in TournamentService.cancelTournament,
        // distinct from the P1 TOURNAMENT_IS_CANCELED invariant checked elsewhere on commit.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)
        tournamentService.cancelTournament(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("cancelTournament"))

        when:
        tournamentService.cancelTournament(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("cancelTournamentAgain"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_IS_CANCELED
    }

    def "deleteTournament: tournament removed, not found via fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — DeleteTournament postconditions
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.deleteTournament(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("deleteTournament"))

        and: 'verify tournament is no longer retrievable'
        tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "deleteTournament: clears participants before removal"() {
        // Spec: plan.md §8 Tournament — rule TOURNAMENT_DELETE (participants must be empty on remove)
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)
        def participant = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def participantDto = userService.getUserById(participant.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        tournamentService.addParticipant(
                tournamentDto.aggregateId,
                participantDto.aggregateId, participantDto.name, participantDto.username, participantDto.version,
                unitOfWorkService.createUnitOfWork("addParticipant"))

        when:
        tournamentService.deleteTournament(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("deleteTournament"))

        then:
        noExceptionThrown()
    }

    def "getOpenTournaments: returns tournaments open for the execution"() {
        // Spec: plan.md §8 Tournament — GetOpenTournaments postconditions
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        def result = tournamentService.getOpenTournaments(executionId, unitOfWorkService.createUnitOfWork("getOpenTournaments"))

        then:
        result.find { it.aggregateId == tournamentDto.aggregateId } != null
    }

    def "removeUserFromTournamentByEvent: participant removed, persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — USER_EXISTS / DeleteUserEvent cached participant removal.
        // Event-driven only (called only via TournamentFunctionalities.*ByEvent from
        // TournamentEventProcessing with an id sourced from an active event subscription) — no
        // not-found path is reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)
        def participant = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def participantDto = userService.getUserById(participant.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        tournamentService.addParticipant(
                tournamentDto.aggregateId,
                participantDto.aggregateId, participantDto.name, participantDto.username, participantDto.version,
                unitOfWorkService.createUnitOfWork("addParticipant"))

        when:
        tournamentService.removeUserFromTournamentByEvent(tournamentDto.aggregateId, participant.aggregateId,
                unitOfWorkService.createUnitOfWork("removeUserFromTournamentByEvent"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        !readBack.participantIds.contains(participant.aggregateId)
    }

    def "removeUserFromTournamentByEvent: creator removal deletes the tournament"() {
        // Spec: plan.md §8 Tournament — creator's DeleteUserEvent removes the whole tournament.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.removeUserFromTournamentByEvent(tournamentDto.aggregateId, userId,
                unitOfWorkService.createUnitOfWork("removeUserFromTournamentByEvent"))

        and: 'verify tournament is no longer retrievable'
        tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateStudentNameByEvent: cached creator name persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — USER_EXISTS / UpdateStudentNameEvent cached-field update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.updateStudentNameByEvent(tournamentDto.aggregateId, userId, USER_NAME_2,
                unitOfWorkService.createUnitOfWork("updateStudentNameByEvent"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.creatorName == USER_NAME_2
    }

    def "anonymizeStudentByEvent: cached creator fields persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — USER_EXISTS / AnonymizeStudentEvent cached-field update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.anonymizeStudentByEvent(tournamentDto.aggregateId, userId, "ANONYMOUS", "ANONYMOUS",
                unitOfWorkService.createUnitOfWork("anonymizeStudentByEvent"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.creatorName == "ANONYMOUS"
        readBack.creatorUsername == "ANONYMOUS"
    }

    def "updateTopicNameByEvent: cached topic name persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — TOPIC_EXISTS / UpdateTopicEvent cached-field update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.updateTopicNameByEvent(tournamentDto.aggregateId, topicId, "Topic A Renamed",
                unitOfWorkService.createUnitOfWork("updateTopicNameByEvent"))

        then: 'read back through a fresh UnitOfWork (TournamentDto has no cached per-topic fields)'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def readBack = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentDto.aggregateId, uow) as Tournament
        readBack.topics.find { it.topicAggregateId == topicId }.topicName == "Topic A Renamed"
    }

    def "removeTopicByEvent: topic removed, persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — TOPIC_EXISTS / DeleteTopicEvent cached-field removal.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.removeTopicByEvent(tournamentDto.aggregateId, topicId,
                unitOfWorkService.createUnitOfWork("removeTopicByEvent"))

        then: 'read back through a fresh UnitOfWork (TournamentDto has no cached per-topic fields)'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def readBack = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentDto.aggregateId, uow) as Tournament
        readBack.topics.find { it.topicAggregateId == topicId } == null
    }

    def "removeTournamentByExecutionByEvent: tournament removed, not found via fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — COURSE_EXECUTION_EXISTS / DeleteCourseExecutionEvent self-removal.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.removeTournamentByExecutionByEvent(tournamentDto.aggregateId,
                unitOfWorkService.createUnitOfWork("removeTournamentByExecutionByEvent"))

        and: 'verify tournament is no longer retrievable'
        tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "removeTournamentByQuizByEvent: tournament removed, not found via fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — QUIZ_EXISTS / InvalidateQuizEvent self-removal.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def tournamentDto = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        when:
        tournamentService.removeTournamentByQuizByEvent(tournamentDto.aggregateId,
                unitOfWorkService.createUnitOfWork("removeTournamentByQuizByEvent"))

        and: 'verify tournament is no longer retrievable'
        tournamentService.getTournamentById(tournamentDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateParticipantAnsweredByEvent: participant answered flag persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — QUIZ_ANSWER_EXISTS / QuizAnswerQuestionAnswerEvent cached update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def readyTournamentId = createStartedTournament(executionId, userId, [topicId], 1).aggregateId
        def participant = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionId, participant.aggregateId)
        addParticipantEnrolledBeforeStart(readyTournamentId,
                participant.aggregateId,
                tournamentFunctionalities.getTournamentById(readyTournamentId).startTime)

        when:
        tournamentService.updateParticipantAnsweredByEvent(readyTournamentId, participant.aggregateId,
                unitOfWorkService.createUnitOfWork("updateParticipantAnsweredByEvent"))

        then: 'read back through a fresh UnitOfWork (TournamentDto has no cached participant-answered field)'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def readBack = unitOfWorkService.aggregateLoadAndRegisterRead(readyTournamentId, uow) as Tournament
        def readParticipant = readBack.participants.find { it.participantAggregateId == participant.aggregateId }
        readParticipant.quizAnswer.answered == true
        readParticipant.quizAnswer.numberOfAnswered == 1
    }

    def "setParticipantQuizAnswer: quiz answer link persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — SolveQuiz postconditions. Relocated from
        // sagas/coordination/tournament/SolveQuizTest's "solveQuiz: success" case (session 3.8):
        // participant.quizAnswer.quizAnswerAggregateId is a Tournament-owned participant-link fact.
        given:
        def readyTournamentId = createStartedTournament(executionId, userId, [topicId], 1).aggregateId
        addParticipantEnrolledBeforeStart(readyTournamentId, userId,
                tournamentFunctionalities.getTournamentById(readyTournamentId).startTime)
        def readyTournamentDto = tournamentFunctionalities.getTournamentById(readyTournamentId)
        def quizAnswerDto = createQuizAnswer(readyTournamentDto.quizAggregateId, userId)

        when:
        tournamentService.setParticipantQuizAnswer(readyTournamentId, userId,
                quizAnswerDto.aggregateId, quizAnswerDto.version,
                unitOfWorkService.createUnitOfWork("setParticipantQuizAnswer"))

        then: 'read back through a fresh UnitOfWork (TournamentDto has no cached participant-link field)'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def readBack = unitOfWorkService.aggregateLoadAndRegisterRead(readyTournamentId, uow) as Tournament
        def readParticipant = readBack.participants.find { it.participantAggregateId == userId }
        readParticipant.quizAnswer.quizAnswerAggregateId == quizAnswerDto.aggregateId
    }

    def "setParticipantQuizAnswer: TOURNAMENT_IS_CANCELED violation"() {
        // Spec: plan.md §8 Tournament — inline service guard in TournamentService.setParticipantQuizAnswer.
        // The guard fires before any participant lookup, so a cancelled tournament with no
        // participant is enough to exercise it. Tournament.verifyInvariants()'s
        // TOURNAMENT_FINAL_AFTER_START rule forbids changing `cancelled` once a tournament has
        // started, so cancellation must happen while the tournament is still in the future.
        given:
        def tournamentId = createTournament(executionId, userId, [topicId], 1, startTime, endTime).aggregateId
        tournamentService.cancelTournament(tournamentId, unitOfWorkService.createUnitOfWork("cancelTournament"))

        when:
        tournamentService.setParticipantQuizAnswer(tournamentId, userId,
                NONEXISTENT_AGGREGATE_ID, 1L,
                unitOfWorkService.createUnitOfWork("setParticipantQuizAnswer"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_IS_CANCELED
    }

    def "setParticipantQuizAnswer: TOURNAMENT_AFTER_END violation"() {
        // Spec: plan.md §8 Tournament — inline service guard in TournamentService.setParticipantQuizAnswer.
        // The guard fires before any participant lookup. A tournament can't be *created* with a past
        // endTime (the saga's generated Quiz would violate its own date-ordering invariant), so the
        // tournament is created with valid future times, then its endTime is pushed into the past via
        // a direct aggregate write — legal because the tournament hasn't started yet (startTime is
        // still in the future), so TOURNAMENT_FINAL_AFTER_START does not apply.
        given:
        def tournamentId = createTournament(executionId, userId, [topicId], 1, startTime, endTime).aggregateId
        def uow = unitOfWorkService.createUnitOfWork("pushTimesToPast")
        def old = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, uow) as Tournament
        def copy = sagasTournamentFactory.createTournamentCopy(old)
        // TOURNAMENT_START_BEFORE_END_TIME (P1) still requires startTime < endTime, so both are
        // pushed into the past together; TOURNAMENT_FINAL_AFTER_START is unaffected because it
        // compares against the tournament's *pre-change* startTime, which was still in the future.
        copy.setStartTime(pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.now().minusMinutes(10))
        copy.setEndTime(pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.now().minusMinutes(5))
        unitOfWorkService.registerChanged(copy, uow)
        unitOfWorkService.commit(uow)

        when:
        tournamentService.setParticipantQuizAnswer(tournamentId, userId,
                NONEXISTENT_AGGREGATE_ID, 1L,
                unitOfWorkService.createUnitOfWork("setParticipantQuizAnswer"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_AFTER_END
    }
}
