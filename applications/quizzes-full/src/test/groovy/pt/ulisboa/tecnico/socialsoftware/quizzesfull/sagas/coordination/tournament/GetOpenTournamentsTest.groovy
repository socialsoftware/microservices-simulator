package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetOpenTournamentsTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId

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
    }

    def "getOpenTournaments: success — returns open tournament for execution"() {
        given: 'a tournament with a future end time'
        def tournament = createTournament(executionId, userId, [topicId], 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2))

        when:
        def result = tournamentFunctionalities.getOpenTournaments(executionId)

        then:
        result.size() == 1
        result[0].aggregateId == tournament.aggregateId
        result[0].executionAggregateId == executionId
    }

    def "getOpenTournaments: cancelled tournament is not returned"() {
        given: 'a tournament that is cancelled'
        def tournament = createTournament(executionId, userId, [topicId], 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2))
        tournamentFunctionalities.cancelTournament(tournament.aggregateId)

        when:
        def result = tournamentFunctionalities.getOpenTournaments(executionId)

        then:
        result.size() == 0
    }

    def "getOpenTournaments: returns empty list for different execution"() {
        given: 'a tournament for executionId'
        createTournament(executionId, userId, [topicId], 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2))

        and: 'a different execution'
        def otherExecution = createExecution(courseId, "SE2025", "2nd Semester 2025")

        when:
        def result = tournamentFunctionalities.getOpenTournaments(otherExecution.aggregateId)

        then:
        result.size() == 0
    }
}
