package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

/**
 * Tests that specifically target the null-collection NPE bug in copy constructors.
 * Every aggregate that has a Set/List collection field is vulnerable: if the
 * aggregate is created with an empty collection and then updated or deleted,
 * the copy constructor calls `other.getX().stream()` which NPEs on null.
 *
 * These tests document which aggregates are affected and which are not.
 */
@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class DeleteBugSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired TopicService topicService
    @Autowired ExecutionService executionService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("delbug-course")
        def req = new CreateCourseRequestDto()
        req.name = "DelBug-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    // --- Execution: has Set<ExecutionUser> users ---

    def "delete Execution with empty users set"() {
        given:
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delbug-exec-create")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [] as Set
            req.acronym = "DELBUG-${System.nanoTime()}"
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("delbug-exec-del")
            executionService.deleteExecution(exec.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
    }

    def "update Execution with empty users set"() {
        given:
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delbug-exec-up-create")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [] as Set
            req.acronym = "DELBUG2-${System.nanoTime()}"
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("delbug-exec-up-get")
            def existing = executionService.getExecutionById(exec.aggregateId, uow2)
            existing.academicTerm = "2026/2027"
            def uow3 = unitOfWorkService.createUnitOfWork("delbug-exec-up-put")
            executionService.updateExecution(existing, uow3)
            unitOfWorkService.commit(uow3)
        then:
            noExceptionThrown()
    }

    // --- Question: has Set<QuestionTopic> topics + List<Option> options ---

    def "delete Question with empty topics and options"() {
        given:
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delbug-q-create")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateQuestionRequestDto()
            req.course = courseRef
            req.topics = [] as Set
            req.title = "DelBugQ-${System.nanoTime()}"
            req.content = "content"
            req.creationDate = LocalDateTime.now()
            req.options = []
            def q = questionService.createQuestion(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("delbug-q-del")
            questionService.deleteQuestion(q.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
    }

    // --- Quiz: has Set<QuizQuestion> questions ---

    def "delete Quiz with empty questions set"() {
        given:
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delbug-quiz-exec")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef
            execReq.users = [] as Set
            execReq.acronym = "DELBUG-QZ-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uow1)
            unitOfWorkService.commit(uow1)

            def uow2 = unitOfWorkService.createUnitOfWork("delbug-quiz-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
            def quizReq = new CreateQuizRequestDto()
            quizReq.execution = execRef
            quizReq.questions = [] as Set
            quizReq.title = "DelBugQuiz-${System.nanoTime()}"
            quizReq.quizType = QuizType.TEST
            quizReq.creationDate = LocalDateTime.now()
            quizReq.availableDate = LocalDateTime.now()
            quizReq.conclusionDate = LocalDateTime.now().plusHours(2)
            quizReq.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(quizReq, uow2)
            unitOfWorkService.commit(uow2)
        when:
            def uow3 = unitOfWorkService.createUnitOfWork("delbug-quiz-del")
            quizService.deleteQuiz(quiz.aggregateId, uow3)
            unitOfWorkService.commit(uow3)
        then:
            noExceptionThrown()
    }
}
