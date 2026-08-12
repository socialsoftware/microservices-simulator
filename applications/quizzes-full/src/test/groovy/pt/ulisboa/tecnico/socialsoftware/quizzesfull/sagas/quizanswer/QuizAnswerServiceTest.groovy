package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.QuizAnswerQuestionAnswerEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUIZ_ANSWER_NOT_FOUND
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.UNIQUE_QUIZ_ANSWER_PER_STUDENT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizAnswerServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    EventService eventService

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId

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
    }

    def "createQuizAnswer: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — CreateQuizAnswer postconditions
        given:
        def quizDto = quizService.getQuizById(quizId, unitOfWorkService.createUnitOfWork("check"))
        def userDto = userService.getUserById(userId, unitOfWorkService.createUnitOfWork("check"))

        when:
        def dto = quizAnswerService.createQuizAnswer(
                quizDto.aggregateId, quizDto.version,
                userDto.aggregateId, userDto.version, userDto.name, userDto.username,
                quizDto.executionId, quizDto.executionVersion,
                unitOfWorkService.createUnitOfWork("createQuizAnswer"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizAnswerService.getQuizAnswerById(dto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.quizAggregateId == quizId
        readBack.userAggregateId == userId
        readBack.executionAggregateId == executionId
        readBack.userName == USER_NAME_1
        readBack.userUsername == USER_USERNAME_1
        readBack.completed == false
        readBack.creationDate != null
        readBack.questionAnswerIds.isEmpty()
    }

    def "createQuizAnswer: UNIQUE_QUIZ_ANSWER_PER_STUDENT violation"() {
        // Spec: plan.md §7 QuizAnswer — rule UNIQUE_QUIZ_ANSWER_PER_STUDENT (P3 own-table uniqueness)
        given:
        createQuizAnswer(quizId, userId)
        def quizDto = quizService.getQuizById(quizId, unitOfWorkService.createUnitOfWork("check"))
        def userDto = userService.getUserById(userId, unitOfWorkService.createUnitOfWork("check"))

        when:
        quizAnswerService.createQuizAnswer(
                quizDto.aggregateId, quizDto.version,
                userDto.aggregateId, userDto.version, userDto.name, userDto.username,
                quizDto.executionId, quizDto.executionVersion,
                unitOfWorkService.createUnitOfWork("createQuizAnswer"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == UNIQUE_QUIZ_ANSWER_PER_STUDENT
    }

    def "getQuizAnswerById: not found throws SimulatorException"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        quizAnswerService.getQuizAnswerById(NONEXISTENT_AGGREGATE_ID, unitOfWorkService.createUnitOfWork("getQuizAnswerById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "answerQuestion: answered question persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — AnswerQuestion postconditions
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.answerQuestion(quizAnswerDto.aggregateId, questionId, 1L, 1, 30,
                unitOfWorkService.createUnitOfWork("answerQuestion"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.questionAnswerIds.size() == 1
        readBack.questionAnswerIds.contains(questionId)
        readBack.completed == false
    }

    def "concludeQuiz: completed flag persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — ConcludeQuiz postconditions
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.concludeQuiz(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("concludeQuiz"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.completed == true
    }

    def "removeQuizAnswer: quiz answer removed, not found via fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — DeleteUserEvent/DeleteCourseExecutionEvent/InvalidateQuizEvent
        // self-removal. Event-driven only (called only from QuizAnswerFunctionalities.removeQuizAnswerByEvent,
        // in turn only called from QuizAnswerEventProcessing with a quiz answer id sourced from its own
        // active event subscription) — no not-found path is reachable through any legitimate caller, so no
        // not-found case is added here.
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.removeQuizAnswer(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("removeQuizAnswer"))

        and: 'verify quiz answer is no longer retrievable'
        quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "removeQuizAnswerIfUserMatches: quiz answer removed when user matches, not found via fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — DisenrollStudentFromCourseExecutionEvent self-removal.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.removeQuizAnswerIfUserMatches(quizAnswerDto.aggregateId, userId,
                unitOfWorkService.createUnitOfWork("removeQuizAnswerIfUserMatches"))

        and: 'verify quiz answer is no longer retrievable'
        quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "removeQuizAnswerIfUserMatches: quiz answer kept when user does not match"() {
        // Spec: plan.md §7 QuizAnswer — DisenrollStudentFromCourseExecutionEvent for an unrelated user
        // must not remove this quiz answer.
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.removeQuizAnswerIfUserMatches(quizAnswerDto.aggregateId, NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("removeQuizAnswerIfUserMatches"))

        then: 'read back through a fresh UnitOfWork — quiz answer is unaffected'
        def readBack = quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.aggregateId == quizAnswerDto.aggregateId
    }

    def "updateStudentName: cached user name persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — USER_EXISTS / UpdateStudentNameEvent cached-field update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.updateStudentName(quizAnswerDto.aggregateId, USER_NAME_2,
                unitOfWorkService.createUnitOfWork("updateStudentName"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.userName == USER_NAME_2
    }

    def "anonymizeStudent: cached user fields persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — USER_EXISTS / AnonymizeStudentEvent cached-field update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.anonymizeStudent(quizAnswerDto.aggregateId, "ANONYMOUS", "ANONYMOUS",
                unitOfWorkService.createUnitOfWork("anonymizeStudent"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.userName == "ANONYMOUS"
        readBack.userUsername == "ANONYMOUS"
    }

    def "updateQuestionVersionInQuizAnswer: cached question version persisted through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — QUESTION_EXISTS / UpdateQuestionEvent cached-field update.
        // Event-driven only — no not-found path reachable through any legitimate caller.
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)
        quizAnswerFunctionalities.answerQuestion(quizAnswerDto.aggregateId, questionId, 1, 30)
        def before = quizAnswerService.getQuizAnswerById(quizAnswerDto.aggregateId, unitOfWorkService.createUnitOfWork("before"))

        when:
        quizAnswerService.updateQuestionVersionInQuizAnswer(quizAnswerDto.aggregateId, questionId, 99L,
                unitOfWorkService.createUnitOfWork("updateQuestionVersionInQuizAnswer"))

        then: 'read back through a second, fresh UnitOfWork (QuizAnswerDto has no cached per-question fields)'
        def uow = unitOfWorkService.createUnitOfWork("check")
        def readBack = unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerDto.aggregateId, uow) as
                pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer
        readBack.questionAnswers.find { it.questionAggregateId == questionId }.questionVersion == 99L
    }

    def "getQuizAnswerByQuizIdAndStudentId: found via composite key"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerByQuizIdAndStudentId postconditions
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        def result = quizAnswerService.getQuizAnswerByQuizIdAndStudentId(quizId, userId,
                unitOfWorkService.createUnitOfWork("getQuizAnswerByQuizIdAndStudentId"))

        then:
        result.aggregateId == quizAnswerDto.aggregateId
        result.quizAggregateId == quizId
        result.userAggregateId == userId
    }

    def "getQuizAnswerByQuizIdAndStudentId: not found throws QuizzesFullException"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerByQuizIdAndStudentId not-found path
        // (Path B: quizAnswerRepository.findByQuizAggregateIdAndUserAggregateId returns empty Optional)
        when:
        quizAnswerService.getQuizAnswerByQuizIdAndStudentId(NONEXISTENT_AGGREGATE_ID, NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getQuizAnswerByQuizIdAndStudentId"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QUIZ_ANSWER_NOT_FOUND
    }

    def "answerQuestion publishes QuizAnswerQuestionAnswerEvent with correct payload"() {
        // Spec: plan.md §7 QuizAnswer — events published (QuizAnswerQuestionAnswerEvent) by AnswerQuestion
        given:
        def quizAnswerDto = createQuizAnswer(quizId, userId)

        when:
        quizAnswerService.answerQuestion(quizAnswerDto.aggregateId, questionId, 1L, 1, 30,
                unitOfWorkService.createUnitOfWork("answerQuestion"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof QuizAnswerQuestionAnswerEvent }
        events.size() == 1
        def event = events[0] as QuizAnswerQuestionAnswerEvent
        event.publisherAggregateId == quizAnswerDto.aggregateId
        event.quizAggregateId == quizId
        event.userAggregateId == userId
    }

    def "createQuizAnswer does not publish any event"() {
        // Negative case: CreateQuizAnswer has no events-published entry in plan.md §7 QuizAnswer
        given:
        def countBefore = eventService.getAllEvents().size()

        when:
        createQuizAnswer(quizId, userId)

        then:
        eventService.getAllEvents().size() == countBefore
    }
}
