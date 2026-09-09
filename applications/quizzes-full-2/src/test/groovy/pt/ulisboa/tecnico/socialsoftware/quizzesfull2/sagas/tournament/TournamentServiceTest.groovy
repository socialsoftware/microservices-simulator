package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DataJpaTest
@Transactional
@Import(TournamentServiceTest.LocalBeanConfiguration)
class TournamentServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_EXECUTION_ACRONYM = "SE-02"
    public static final Integer SECOND_NUMBER_OF_QUESTIONS = 8
    public static final String SEEDED_QUESTION_TITLE = "Seeded question"
    public static final LocalDateTime UPDATED_START_TIME = DateHandler.now().truncatedTo(ChronoUnit.MICROS).plusDays(20)
    public static final LocalDateTime UPDATED_END_TIME = DateHandler.now().truncatedTo(ChronoUnit.MICROS).plusDays(20).plusHours(3)

    @Autowired
    EventService eventService


    def "getTournamentById: reads back the persisted tournament through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — GetTournamentById; fields startTime, endTime,
        // numberOfQuestions, cancelled, execution, creator, participants, topics
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)

        when:
        flushAndClear()
        def result = tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == tournamentAggregateId
        result.startTime == TOURNAMENT_START_TIME
        result.endTime == TOURNAMENT_END_TIME
        result.numberOfQuestions == TOURNAMENT_NUMBER_OF_QUESTIONS
        !result.cancelled
        result.executionAggregateId == executionAggregateId
        result.executionVersion != null
        result.courseAggregateId == courseAggregateId
        result.creatorAggregateId == creatorAggregateId
        result.creatorName == USER_NAME
        result.creatorUsername == USER_USERNAME
        result.creatorVersion != null
        result.participants.isEmpty()
        result.topics.isEmpty()
        result.version != null
    }

    def "getTournamentById: the topic snapshot survives the load path"() {
        // Spec: plan.md §8 Tournament — TournamentTopic caches topicAggregateId, topicName,
        // topicVersion, courseAggregateId (grouping §2)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def topicAggregateId = createTopic(courseAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS,
                [topicAggregateId])

        when:
        flushAndClear()
        def result = tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.topics.size() == 1
        result.topics[0].topicAggregateId == topicAggregateId
        result.topics[0].topicName == TOPIC_NAME
        result.topics[0].topicVersion != null
        result.topics[0].courseAggregateId == courseAggregateId
    }

    def "getTournamentById: unknown aggregate id is not found"() {
        // Spec: plan.md §8 Tournament — Path A (aggregateLoadAndRegisterRead)
        when:
        tournamentService.getTournamentById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getTournamentsForExecution: returns every tournament of the execution with its own state"() {
        // Spec: plan.md §8 Tournament — GetTournamentsForExecution(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def firstAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        def secondAggregateId = createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, SECOND_NUMBER_OF_QUESTIONS)

        when:
        flushAndClear()
        def result = tournamentService.getTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.numberOfQuestions == TOURNAMENT_NUMBER_OF_QUESTIONS
        first.executionAggregateId == executionAggregateId
        def second = result.find { it.aggregateId == secondAggregateId }
        second.numberOfQuestions == SECOND_NUMBER_OF_QUESTIONS
        second.executionAggregateId == executionAggregateId
    }

    def "getTournamentsForExecution: excludes tournaments belonging to another execution"() {
        // Spec: plan.md §8 Tournament — GetTournamentsForExecution filters on the cached
        // executionAggregateId
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def otherExecutionAggregateId = createExecution(courseAggregateId, SECOND_EXECUTION_ACRONYM)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        enrollStudentInExecution(otherExecutionAggregateId, creatorAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        createTournament(otherExecutionAggregateId, creatorAggregateId)

        when:
        flushAndClear()
        def result = tournamentService.getTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [tournamentAggregateId]
    }

    def "getTournamentsForExecution: returns an empty list when the execution has no tournament"() {
        // Spec: plan.md §8 Tournament — GetTournamentsForExecution(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        def result = tournamentService.getTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "getOpenedTournamentsForExecution: keeps a tournament whose end time has not passed"() {
        // Spec: plan.md §8 Tournament — GetOpenedTournamentsForExecution; open = not cancelled and
        // endTime after now
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def openAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        createClosedTournament(executionAggregateId, creatorAggregateId)

        when:
        flushAndClear()
        def result = tournamentService.getOpenedTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [openAggregateId]
        result[0].endTime == TOURNAMENT_END_TIME
    }

    def "getClosedTournamentsForExecution: keeps a tournament whose end time has passed"() {
        // Spec: plan.md §8 Tournament — GetClosedTournamentsForExecution; closed = not cancelled and
        // endTime not after now
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        createTournament(executionAggregateId, creatorAggregateId)
        def closedAggregateId = createClosedTournament(executionAggregateId, creatorAggregateId)

        when:
        flushAndClear()
        def result = tournamentService.getClosedTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [closedAggregateId]
        !result[0].endTime.isAfter(DateHandler.now())
    }

    def "opened and closed exclude a cancelled tournament that getTournamentsForExecution still returns"() {
        // Spec: plan.md §8 Tournament — a cancelled tournament is neither open nor closed
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def cancelledAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        cancelTournament(cancelledAggregateId)

        when:
        flushAndClear()
        def all = tournamentService.getTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        def opened = tournamentService.getOpenedTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        def closed = tournamentService.getClosedTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        all.collect { it.aggregateId } == [cancelledAggregateId]
        all[0].cancelled
        opened.isEmpty()
        closed.isEmpty()
    }

    def "createTournament: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — CreateTournament(executionAggregateId, creatorAggregateId,
        // startTime, endTime, numberOfQuestions, topicAggregateIds) postconditions
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def topicAggregateId = createTopic(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)
        seedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS, [topicAggregateId])

        when:
        def dto = tournamentService.createTournament(executionDtoOf(executionAggregateId),
                userDtoOf(creatorAggregateId), [tournamentTopicDtoOf(topicAggregateId)],
                selectedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS), quizAggregateId,
                TOURNAMENT_QUIZ_VERSION, TOURNAMENT_START_TIME, TOURNAMENT_END_TIME,
                TOURNAMENT_NUMBER_OF_QUESTIONS, unitOfWorkService.createUnitOfWork("createTournament"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = tournamentService.getTournamentById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.startTime == TOURNAMENT_START_TIME
        readBack.endTime == TOURNAMENT_END_TIME
        readBack.numberOfQuestions == TOURNAMENT_NUMBER_OF_QUESTIONS
        !readBack.cancelled
        readBack.executionAggregateId == executionAggregateId
        readBack.courseAggregateId == courseAggregateId
        readBack.creatorAggregateId == creatorAggregateId
        readBack.creatorName == USER_NAME
        readBack.creatorUsername == USER_USERNAME
        readBack.quizAggregateId == quizAggregateId
        readBack.quizVersion == TOURNAMENT_QUIZ_VERSION
        readBack.topics.collect { it.topicAggregateId } == [topicAggregateId]
        readBack.participants.isEmpty()
    }

    def "addParticipant: the participant is persisted with its enrolment snapshot"() {
        // Spec: plan.md §8 Tournament — AddParticipant(tournamentAggregateId, userAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def participantAggregateId = createActiveUser(TOURNAMENT_PARTICIPANT_NAME,
                TOURNAMENT_PARTICIPANT_USERNAME)
        enrollStudentInExecution(executionAggregateId, participantAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)

        when:
        tournamentService.addParticipant(tournamentAggregateId, userDtoOf(participantAggregateId),
                executionDtoOf(executionAggregateId),
                unitOfWorkService.createUnitOfWork("addParticipant"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.participants.size() == 1
        readBack.participants[0].userAggregateId == participantAggregateId
        readBack.participants[0].userName == TOURNAMENT_PARTICIPANT_NAME
        readBack.participants[0].userUsername == TOURNAMENT_PARTICIPANT_USERNAME
        readBack.participants[0].userVersion != null
        readBack.participants[0].enrollTime != null
        readBack.participants[0].enrollTime.isBefore(TOURNAMENT_START_TIME)
    }

    def "updateTournament: the new schedule and topic set are persisted"() {
        // Spec: plan.md §8 Tournament — UpdateTournament(tournamentAggregateId, startTime, endTime,
        // numberOfQuestions, topicAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        def topicAggregateId = createTopic(courseAggregateId)
        seedQuestions(courseAggregateId, SECOND_NUMBER_OF_QUESTIONS, [topicAggregateId])

        when:
        tournamentService.updateTournament(tournamentAggregateId, UPDATED_START_TIME, UPDATED_END_TIME,
                SECOND_NUMBER_OF_QUESTIONS, [tournamentTopicDtoOf(topicAggregateId)],
                selectedQuestions(courseAggregateId, SECOND_NUMBER_OF_QUESTIONS),
                unitOfWorkService.createUnitOfWork("updateTournament"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.startTime == UPDATED_START_TIME
        readBack.endTime == UPDATED_END_TIME
        readBack.numberOfQuestions == SECOND_NUMBER_OF_QUESTIONS
        readBack.topics.collect { it.topicAggregateId } == [topicAggregateId]
    }

    def "cancelTournament: the cancelled flag is persisted"() {
        // Spec: plan.md §8 Tournament — CancelTournament(tournamentAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)

        when:
        tournamentService.cancelTournament(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("cancelTournament"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.cancelled
    }

    def "deleteTournament: a tournament with a participant is cleared and no longer resolves"() {
        // Spec: plan.md §8 Tournament — DeleteTournament(tournamentAggregateId), clearing its
        // participant list in the same operation so that TOURNAMENT_DELETE holds
        given: 'a tournament that has a participant, so an uncleared delete would trip TOURNAMENT_DELETE'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def participantAggregateId = createActiveUser(TOURNAMENT_PARTICIPANT_NAME,
                TOURNAMENT_PARTICIPANT_USERNAME)
        enrollStudentInExecution(executionAggregateId, participantAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)
        tournamentService.addParticipant(tournamentAggregateId, userDtoOf(participantAggregateId),
                executionDtoOf(executionAggregateId),
                unitOfWorkService.createUnitOfWork("addParticipant"))

        when: 'the delete clears the participants and only then removes the aggregate'
        tournamentService.deleteTournament(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("deleteTournament"))
        flushAndClear()
        tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then: 'a DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "createTournament: CREATOR_COURSE_EXECUTION violation"() {
        // Spec: plan.md §8 Tournament — rule CREATOR_COURSE_EXECUTION (P3 DTO-field guard)
        given: 'a creator who was never enrolled in the execution'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        seedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        when:
        tournamentService.createTournament(executionDtoOf(executionAggregateId),
                userDtoOf(creatorAggregateId), [],
                selectedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS), quizAggregateId,
                TOURNAMENT_QUIZ_VERSION, TOURNAMENT_START_TIME, TOURNAMENT_END_TIME,
                TOURNAMENT_NUMBER_OF_QUESTIONS, unitOfWorkService.createUnitOfWork("createTournament"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.CREATOR_COURSE_EXECUTION
    }

    def "addParticipant: PARTICIPANT_COURSE_EXECUTION violation"() {
        // Spec: plan.md §8 Tournament — rule PARTICIPANT_COURSE_EXECUTION (P3 half, at add time)
        given: 'a user who was never enrolled in the tournament execution'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def outsiderAggregateId = createActiveUser(TOURNAMENT_PARTICIPANT_NAME,
                TOURNAMENT_PARTICIPANT_USERNAME)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)

        when:
        tournamentService.addParticipant(tournamentAggregateId, userDtoOf(outsiderAggregateId),
                executionDtoOf(executionAggregateId),
                unitOfWorkService.createUnitOfWork("addParticipant"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.PARTICIPANT_COURSE_EXECUTION
    }

    // BVA pair on the ordered-domain count guard TOURNAMENT_NOT_ENOUGH_QUESTIONS: on-point is exactly
    // numberOfQuestions selected, off-point is one short.
    def "createTournament: TOURNAMENT_NOT_ENOUGH_QUESTIONS on-point — exactly numberOfQuestions"() {
        // Spec: plan.md §8 Tournament — rule TOURNAMENT_NOT_ENOUGH_QUESTIONS (P3 count guard)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)
        seedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        when:
        tournamentService.createTournament(executionDtoOf(executionAggregateId),
                userDtoOf(creatorAggregateId), [],
                selectedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS), quizAggregateId,
                TOURNAMENT_QUIZ_VERSION, TOURNAMENT_START_TIME, TOURNAMENT_END_TIME,
                TOURNAMENT_NUMBER_OF_QUESTIONS, unitOfWorkService.createUnitOfWork("createTournament"))

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "createTournament: TOURNAMENT_NOT_ENOUGH_QUESTIONS off-point — one short"() {
        // Spec: plan.md §8 Tournament — rule TOURNAMENT_NOT_ENOUGH_QUESTIONS (P3 count guard)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)
        seedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS - 1, [])

        when:
        tournamentService.createTournament(executionDtoOf(executionAggregateId),
                userDtoOf(creatorAggregateId), [],
                selectedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS), quizAggregateId,
                TOURNAMENT_QUIZ_VERSION, TOURNAMENT_START_TIME, TOURNAMENT_END_TIME,
                TOURNAMENT_NUMBER_OF_QUESTIONS, unitOfWorkService.createUnitOfWork("createTournament"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_NOT_ENOUGH_QUESTIONS
    }

    def "updateTournament: TOURNAMENT_NOT_ENOUGH_QUESTIONS violation"() {
        // Spec: plan.md §8 Tournament — rule TOURNAMENT_NOT_ENOUGH_QUESTIONS, update path
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId)

        when: 'the new topic set yields fewer questions than the new numberOfQuestions'
        tournamentService.updateTournament(tournamentAggregateId, UPDATED_START_TIME, UPDATED_END_TIME,
                SECOND_NUMBER_OF_QUESTIONS, [],
                selectedQuestions(courseAggregateId, SECOND_NUMBER_OF_QUESTIONS),
                unitOfWorkService.createUnitOfWork("updateTournament"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_NOT_ENOUGH_QUESTIONS
    }

    // Tournament's "Events published" list in plan.md §8 is empty, so this class carries no
    // payload-asserting event case. The class-scoped negative case below stands in for the whole set.
    def "createTournament publishes no event"() {
        // Spec: plan.md §8 Tournament — Events published: —
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def creatorAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)
        seedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS, [])
        def countBefore = eventService.getAllEvents().size()

        when:
        tournamentService.createTournament(executionDtoOf(executionAggregateId),
                userDtoOf(creatorAggregateId), [],
                selectedQuestions(courseAggregateId, TOURNAMENT_NUMBER_OF_QUESTIONS), quizAggregateId,
                TOURNAMENT_QUIZ_VERSION, TOURNAMENT_START_TIME, TOURNAMENT_END_TIME,
                TOURNAMENT_NUMBER_OF_QUESTIONS, unitOfWorkService.createUnitOfWork("createTournament"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

    private void seedQuestions(Integer courseAggregateId, Integer count, List<Integer> topicAggregateIds) {
        count.times { index ->
            createQuestion(courseAggregateId, SEEDED_QUESTION_TITLE + " " + index, QUESTION_CONTENT,
                    topicAggregateIds)
        }
    }

    // The saga assembles this from the questions its selection step drew; a direct service test does
    // the same fetch and truncation itself.
    private List<QuizQuestionDto> selectedQuestions(Integer courseAggregateId, Integer count) {
        return questionService.getQuestionsByCourse(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
                .take(count)
                .collect { new QuizQuestionDto(it.aggregateId, it.version, it.title, it.content) }
    }

    private ExecutionDto executionDtoOf(Integer executionAggregateId) {
        return executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
    }

    private UserDto userDtoOf(Integer userAggregateId) {
        return userService.getUserById(userAggregateId, unitOfWorkService.createUnitOfWork("check"))
    }

    private TournamentTopicDto tournamentTopicDtoOf(Integer topicAggregateId) {
        def topicDto = topicService.getTopicById(topicAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        return new TournamentTopicDto(topicDto.aggregateId, topicDto.name, topicDto.version,
                topicDto.courseAggregateId)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
