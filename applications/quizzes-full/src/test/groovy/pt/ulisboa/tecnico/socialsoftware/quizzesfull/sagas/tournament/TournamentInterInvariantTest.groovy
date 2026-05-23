package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.handling.TournamentEventHandling

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TournamentInterInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    TournamentEventHandling tournamentEventHandling

    Integer courseId
    Integer creatorId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer tournamentId
    LocalDateTime startTime
    LocalDateTime endTime

    def setup() {
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        courseId = course.aggregateId

        def creator = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        creatorId = creator.aggregateId

        def execution = createExecution(courseId, "SE2024", "1st Semester 2024")
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, creatorId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        def question = createQuestion(courseId, [topicId], "Q1", "Content 1")
        questionId = question.aggregateId

        startTime = LocalDateTime.now().plusDays(1)
        endTime = LocalDateTime.now().plusDays(2)

        def tournament = createTournament(executionId, creatorId, [topicId], 1, startTime, endTime)
        tournamentId = tournament.aggregateId
    }

    // ─── CREATOR_EXISTS / PARTICIPANT_EXISTS — DeleteUserEvent ────────────────

    def "tournament is deleted on DeleteUserEvent for creator"() {
        when: 'creator is deleted, publishing DeleteUserEvent'
        userFunctionalities.deleteUser(creatorId)

        and: 'tournament polls for delete user events'
        tournamentEventHandling.handleDeleteUserEvents()

        and: 'attempt to load the now-deleted tournament'
        unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "tournament ignores DeleteUserEvent for unrelated user"() {
        given:
        def unrelated = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)

        when: 'unrelated user is deleted'
        userFunctionalities.deleteUser(unrelated.aggregateId)

        and: 'tournament polls for delete user events'
        tournamentEventHandling.handleDeleteUserEvents()

        then: 'tournament is still active'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, uow)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── CREATOR_EXISTS / PARTICIPANT_EXISTS — UpdateStudentNameEvent ─────────

    def "tournament reflects UpdateStudentNameEvent for creator"() {
        when: 'creator name is updated, publishing UpdateStudentNameEvent'
        def uow = unitOfWorkService.createUnitOfWork("updateName")
        userService.updateUserName(creatorId, USER_NAME_2, uow)
        unitOfWorkService.commit(uow)

        and: 'tournament polls for update student name events'
        tournamentEventHandling.handleUpdateStudentNameEvents()

        then: 'cached creator name in tournament is updated'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def updated = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        updated.creatorName == USER_NAME_2
    }

    def "tournament ignores UpdateStudentNameEvent for unrelated user"() {
        given:
        def unrelated = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)

        when: 'unrelated user name is updated'
        def uow = unitOfWorkService.createUnitOfWork("updateName")
        userService.updateUserName(unrelated.aggregateId, "New Name", uow)
        unitOfWorkService.commit(uow)

        and: 'tournament polls for update student name events'
        tournamentEventHandling.handleUpdateStudentNameEvents()

        then: 'creator name in tournament is unchanged'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        unchanged.creatorName == USER_NAME_1
    }

    // ─── CREATOR_EXISTS / PARTICIPANT_EXISTS — AnonymizeStudentEvent ──────────

    def "tournament reflects AnonymizeStudentEvent for creator"() {
        when: 'creator is anonymized, publishing AnonymizeStudentEvent'
        def uow = unitOfWorkService.createUnitOfWork("anonymize")
        userService.anonymizeUser(creatorId, uow)
        unitOfWorkService.commit(uow)

        and: 'tournament polls for anonymize student events'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'cached creator name and username in tournament are anonymized'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def updated = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        updated.creatorName == "ANONYMOUS"
        updated.creatorUsername == "ANONYMOUS"
    }

    def "tournament ignores AnonymizeStudentEvent for unrelated user"() {
        given:
        def unrelated = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)

        when: 'unrelated user is anonymized'
        def uow = unitOfWorkService.createUnitOfWork("anonymize")
        userService.anonymizeUser(unrelated.aggregateId, uow)
        unitOfWorkService.commit(uow)

        and: 'tournament polls for anonymize student events'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'creator name and username in tournament are unchanged'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        unchanged.creatorName == USER_NAME_1
        unchanged.creatorUsername == USER_USERNAME_1
    }

    // ─── TOPIC_EXISTS — UpdateTopicEvent ─────────────────────────────────────

    def "tournament reflects UpdateTopicEvent"() {
        when: 'topic name is updated, publishing UpdateTopicEvent'
        def topicDto = new TopicDto()
        topicDto.aggregateId = topicId
        topicDto.name = "Updated Topic"
        topicFunctionalities.updateTopic(topicDto)

        and: 'tournament polls for update topic events'
        tournamentEventHandling.handleUpdateTopicEvents()

        then: 'cached topic name in tournament is updated'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def updated = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        updated.topics.any { it.topicAggregateId == topicId && it.topicName == "Updated Topic" }
    }

    def "tournament ignores UpdateTopicEvent for unrelated topic"() {
        given:
        def topic2 = createTopic(courseId, "Topic B")

        when: 'unrelated topic name is updated'
        def topicDto = new TopicDto()
        topicDto.aggregateId = topic2.aggregateId
        topicDto.name = "Updated B"
        topicFunctionalities.updateTopic(topicDto)

        and: 'tournament polls for update topic events'
        tournamentEventHandling.handleUpdateTopicEvents()

        then: 'tournament topic name is unchanged'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        unchanged.topics.any { it.topicAggregateId == topicId && it.topicName == "Topic A" }
    }

    // ─── TOPIC_EXISTS — DeleteTopicEvent ─────────────────────────────────────

    def "tournament removes topic on DeleteTopicEvent"() {
        when: 'topic is deleted, publishing DeleteTopicEvent'
        topicFunctionalities.deleteTopic(topicId)

        and: 'tournament polls for delete topic events'
        tournamentEventHandling.handleDeleteTopicEvents()

        then: 'topic is removed from tournament'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def updated = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        updated.topics.every { it.topicAggregateId != topicId }
    }

    def "tournament ignores DeleteTopicEvent for unrelated topic"() {
        given:
        def topic2 = createTopic(courseId, "Topic B")

        when: 'unrelated topic is deleted'
        topicFunctionalities.deleteTopic(topic2.aggregateId)

        and: 'tournament polls for delete topic events'
        tournamentEventHandling.handleDeleteTopicEvents()

        then: 'tournament still contains its topic'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, checkUow) as Tournament
        unchanged.topics.any { it.topicAggregateId == topicId }
    }

    // ─── COURSE_EXECUTION_EXISTS — DeleteCourseExecutionEvent ─────────────────

    def "tournament is deleted on DeleteCourseExecutionEvent"() {
        when: 'execution is deleted, publishing DeleteCourseExecutionEvent'
        executionFunctionalities.deleteExecution(executionId)

        and: 'tournament polls for delete course execution events'
        tournamentEventHandling.handleDeleteCourseExecutionEvents()

        and: 'attempt to load the now-deleted tournament'
        unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "tournament ignores DeleteCourseExecutionEvent for unrelated execution"() {
        given:
        def execution2 = createExecution(courseId, "SE2025", "2nd Semester 2024")

        when: 'unrelated execution is deleted'
        executionFunctionalities.deleteExecution(execution2.aggregateId)

        and: 'tournament polls for delete course execution events'
        tournamentEventHandling.handleDeleteCourseExecutionEvents()

        then: 'tournament is still active'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, uow)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── QUIZ_EXISTS — InvalidateQuizEvent ───────────────────────────────────

    def "tournament is deleted on InvalidateQuizEvent"() {
        given:
        def quizId = tournamentFunctionalities.getTournamentById(tournamentId).quizAggregateId

        when: 'tournament quiz is invalidated, publishing InvalidateQuizEvent'
        quizFunctionalities.invalidateQuizByEvent(quizId)

        and: 'tournament polls for invalidate quiz events'
        tournamentEventHandling.handleInvalidateQuizEvents()

        and: 'attempt to load the now-deleted tournament'
        unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "tournament ignores InvalidateQuizEvent for unrelated quiz"() {
        given:
        def quiz2 = createQuiz(executionId, [questionId])

        when: 'unrelated quiz is invalidated'
        quizFunctionalities.invalidateQuizByEvent(quiz2.aggregateId)

        and: 'tournament polls for invalidate quiz events'
        tournamentEventHandling.handleInvalidateQuizEvents()

        then: 'tournament is still active'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(tournamentId, uow)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── QUIZ_ANSWER_EXISTS — QuizAnswerQuestionAnswerEvent ───────────────────

    private Integer prepareTournamentReadyForSolveQuiz() {
        def tournament = createStartedTournament(executionId, creatorId, [topicId], 1)
        addParticipantEnrolledBeforeStart(tournament.aggregateId, creatorId, tournament.startTime)
        return tournament.aggregateId
    }

    def "tournament reflects QuizAnswerQuestionAnswerEvent for linked participant"() {
        given: 'creator is added as participant and a quiz answer is linked on a started tournament'
        def readyTournamentId = prepareTournamentReadyForSolveQuiz()
        def tournamentDto = tournamentFunctionalities.getTournamentById(readyTournamentId)
        def quizAnswer = createQuizAnswer(tournamentDto.quizAggregateId, creatorId)
        tournamentFunctionalities.solveQuiz(readyTournamentId, creatorId)

        when: 'participant answers a question, publishing QuizAnswerQuestionAnswerEvent'
        quizAnswerFunctionalities.answerQuestion(quizAnswer.aggregateId, questionId, 1, 30)

        and: 'tournament polls for quiz answer question answer events'
        tournamentEventHandling.handleQuizAnswerQuestionAnswerEvents()

        then: 'participant answered flag is set and answer count reflects the event'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def updated = unitOfWorkService.aggregateLoadAndRegisterRead(readyTournamentId, checkUow) as Tournament
        def participant = updated.participants.find { it.participantAggregateId == creatorId }
        participant != null && participant.quizAnswer.answered == true
        participant.quizAnswer.numberOfAnswered == 1
    }

    def "tournament ignores QuizAnswerQuestionAnswerEvent for unlinked quiz answer"() {
        given: 'creator is added as participant with a linked quiz answer on a started tournament'
        def readyTournamentId = prepareTournamentReadyForSolveQuiz()
        def tournamentDto = tournamentFunctionalities.getTournamentById(readyTournamentId)
        createQuizAnswer(tournamentDto.quizAggregateId, creatorId)
        tournamentFunctionalities.solveQuiz(readyTournamentId, creatorId)

        and: 'a second user creates an unlinked quiz answer'
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionId, user2.aggregateId)
        def quiz2 = createQuiz(executionId, [questionId])
        def quizAnswer2 = createQuizAnswer(quiz2.aggregateId, user2.aggregateId)

        when: 'second user answers a question on the unlinked quiz answer'
        quizAnswerFunctionalities.answerQuestion(quizAnswer2.aggregateId, questionId, 1, 30)

        and: 'tournament polls for quiz answer question answer events'
        tournamentEventHandling.handleQuizAnswerQuestionAnswerEvents()

        then: 'creator participant answer count is unchanged by the unlinked event'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def unchanged = unitOfWorkService.aggregateLoadAndRegisterRead(readyTournamentId, checkUow) as Tournament
        def participant = unchanged.participants.find { it.participantAggregateId == creatorId }
        participant != null && participant.quizAnswer.numberOfAnswered == 0
    }
}
