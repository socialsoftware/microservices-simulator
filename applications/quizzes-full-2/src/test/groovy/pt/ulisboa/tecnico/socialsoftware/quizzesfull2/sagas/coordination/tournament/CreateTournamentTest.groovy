package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception

@DataJpaTest
@Transactional
@Import(CreateTournamentTest.LocalBeanConfiguration)
class CreateTournamentTest extends QuizzesFull2SpockTest {

    Integer courseAggregateId
    Integer executionAggregateId
    Integer creatorAggregateId

    def setup() {
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        creatorAggregateId = createActiveUser()
    }

    def "createTournament: success"() {
        // Spec: plan.md §8 Tournament — CreateTournament(executionAggregateId, creatorAggregateId,
        // startTime, endTime, numberOfQuestions, topicAggregateIds); happy path
        given: 'an enrolled creator and a course holding enough questions for the tournament topic'
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        def topicAggregateId = createTopic(courseAggregateId)
        seedQuestions(TOURNAMENT_NUMBER_OF_QUESTIONS, [topicAggregateId])

        when:
        def result = tournamentFunctionalities.createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS,
                [topicAggregateId])

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.executionAggregateId == executionAggregateId
        result.creatorAggregateId == creatorAggregateId
        result.numberOfQuestions == TOURNAMENT_NUMBER_OF_QUESTIONS
        result.topics.collect { it.topicAggregateId } == [topicAggregateId]
        result.quizAggregateId != null
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createTournament: CREATOR_COURSE_EXECUTION violation"() {
        // Spec: plan.md §8 Tournament — rule CREATOR_COURSE_EXECUTION
        given: 'a creator who was never enrolled in the execution'
        seedQuestions(TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        when:
        tournamentFunctionalities.createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.CREATOR_COURSE_EXECUTION
    }

    def "createTournament: TOURNAMENT_NOT_ENOUGH_QUESTIONS violation"() {
        // Spec: plan.md §8 Tournament — rule TOURNAMENT_NOT_ENOUGH_QUESTIONS
        given: 'a course one question short of what the tournament asks for'
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        seedQuestions(TOURNAMENT_NUMBER_OF_QUESTIONS - 1, [])

        when:
        tournamentFunctionalities.createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, TOURNAMENT_NUMBER_OF_QUESTIONS, [])

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_NOT_ENOUGH_QUESTIONS
    }

    private void seedQuestions(Integer count, List<Integer> topicAggregateIds) {
        count.times { index ->
            createQuestion(courseAggregateId, QUESTION_TITLE + " " + index, QUESTION_CONTENT,
                    topicAggregateIds)
        }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
