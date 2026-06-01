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
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_TOPIC_COURSE_MISMATCH

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
        // Spec: Tournament.{executionAggregateId,creatorAggregateId,numberOfQuestions} = input;
        //       cancelled=false; SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.8 Tournament / createTournament.
        when:
        def result = createTournament(executionId, userId, [topicId], 1, startTime, endTime)

        then:
        result.aggregateId != null
        result.executionAggregateId == executionId
        result.creatorAggregateId == userId
        result.numberOfQuestions == 1
        result.cancelled == false
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

    def "createTournament: TOPIC_COURSE_EXECUTION violation — topic from different course"() {
        given: 'a topic from a different course'
        def otherCourse = createCourse("Other Course", "TECNICO")
        def otherTopic = createTopic(otherCourse.aggregateId, "Other Topic")

        when:
        createTournament(executionId, userId, [otherTopic.aggregateId], 1, startTime, endTime)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == TOURNAMENT_TOPIC_COURSE_MISMATCH
    }

    def "createTournament: TOURNAMENT_START_BEFORE_END_TIME violation"() {
        given: 'start time after end time'
        def badStart = LocalDateTime.now().plusDays(3)
        def badEnd = LocalDateTime.now().plusDays(1)

        when:
        createTournament(executionId, userId, [topicId], 1, badStart, badEnd)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_START_BEFORE_END_TIME
    }

    def "createTournament: TOURNAMENT_START_BEFORE_END_TIME boundary — startTime == endTime"() {
        // Spec: plan.md §2.8 Tournament — TOURNAMENT_START_BEFORE_END_TIME (startTime < endTime).
        // BVA off-point: the equal instant is the first violating value (the case above is a far
        // representative). See docs/concepts/testing.md § Choosing Input Values.
        given: 'start time equal to end time'
        def instant = LocalDateTime.now().plusDays(1)

        when:
        createTournament(executionId, userId, [topicId], 1, instant, instant)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_START_BEFORE_END_TIME
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
