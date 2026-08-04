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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND
// P1 intra-invariant violations are NOT tested here — see TournamentIntraInvariantTest.
// TOPIC_COURSE_EXECUTION (P3 service guard) is tested directly against TournamentService in
// TournamentServiceTest — not through the saga path here.

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateTournamentTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
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
    }

    def "createTournament: success"() {
        // Spec: plan.md §8 Tournament / createTournament. Orchestration outcome only —
        // persistence is asserted in TournamentServiceTest.
        when:
        def result = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        then:
        result.aggregateId != null
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createTournament: CREATOR_COURSE_EXECUTION violation — creator not enrolled"() {
        given: 'a user who is not enrolled in the execution'
        def otherUser = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def otherUserId = otherUser.aggregateId

        when:
        createTournament(executionId, otherUserId, [topicId], 1, startTime, endTime)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == COURSE_EXECUTION_STUDENT_NOT_FOUND
    }

    def "createTournament: getStudentStep enforces CREATOR_COURSE_EXECUTION check"() {
        given: 'workflow paused after getExecutionStep'
        def uow = unitOfWorkService.createUnitOfWork("createTournament")
        def otherUser = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def func = new CreateTournamentFunctionalitySagas(
                unitOfWorkService, executionId, otherUser.aggregateId, [topicId], 1,
                startTime, endTime, uow, commandGateway)

        when:
        func.executeUntilStep("getStudentStep", uow)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == COURSE_EXECUTION_STUDENT_NOT_FOUND
    }
}
