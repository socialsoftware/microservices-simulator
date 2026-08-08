package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(TournamentServiceTest.LocalBeanConfiguration)
class TournamentServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_EXECUTION_ACRONYM = "SE-02"
    public static final Integer SECOND_NUMBER_OF_QUESTIONS = 8

    // A tournament whose window has already closed. Pinned against DateHandler.now(), the clock the
    // service's open/closed predicate reads.
    public static final LocalDateTime PAST_START_TIME = DateHandler.now().minusDays(2)
    public static final LocalDateTime PAST_END_TIME = DateHandler.now().minusDays(1)

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
        createTournament(executionAggregateId, creatorAggregateId, PAST_START_TIME, PAST_END_TIME)

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
        def closedAggregateId = createTournament(executionAggregateId, creatorAggregateId,
                PAST_START_TIME, PAST_END_TIME)

        when:
        flushAndClear()
        def result = tournamentService.getClosedTournamentsForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [closedAggregateId]
        result[0].endTime == PAST_END_TIME
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

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
