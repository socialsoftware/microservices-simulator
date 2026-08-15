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
class GetTournamentByIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer tournamentId

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

        def tournament = createTournament(executionId, userId, [topicId], 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2))
        tournamentId = tournament.aggregateId
    }

    def "getTournamentById: success"() {
        when:
        def result = tournamentFunctionalities.getTournamentById(tournamentId)

        then:
        result.aggregateId == tournamentId
        result.executionAggregateId == executionId
        result.creatorAggregateId == userId
        result.numberOfQuestions == 1
        result.cancelled == false
    }

    // "aggregate not found" case relocated to TournamentServiceTest (Path A SimulatorException,
    // session 3.8) — no longer duplicated here.
}
