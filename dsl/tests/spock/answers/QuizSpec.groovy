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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class QuizSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("quiz-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "SE-quiz-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("quiz-seed-exec")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateExecutionRequestDto()
        req.course = courseRef
        req.users = [] as Set
        req.acronym = "QZ-${System.nanoTime()}"
        req.academicTerm = "2025/2026"
        req.endDate = LocalDateTime.now().plusMonths(3)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course, String name) {
        def uow = unitOfWorkService.createUnitOfWork("quiz-seed-topic-$name")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateTopicRequestDto()
        req.course = courseRef
        req.name = name
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course, TopicDto topic, String title) {
        def uow = unitOfWorkService.createUnitOfWork("quiz-seed-q-$title")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def topicRef = new TopicDto()
        topicRef.aggregateId = topic.aggregateId
        topicRef.name = topic.name
        def opt = new OptionDto()
        opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
        def req = new CreateQuestionRequestDto()
        req.course = courseRef
        req.topics = [topicRef] as Set
        req.title = title
        req.content = "content"
        req.creationDate = LocalDateTime.now()
        req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "createQuiz with execution and questions persists correctly"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course, "Sorting-quiz")
            def question = seedQuestion(course, topic, "Is BFS optimal?")
        when:
            def uow = unitOfWorkService.createUnitOfWork("quiz-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
            def qRef = new QuestionDto()
            qRef.aggregateId = question.aggregateId
            qRef.title = question.title
            def req = new CreateQuizRequestDto()
            req.execution = execRef
            req.questions = [qRef] as Set
            req.title = "Midterm Quiz"
            req.quizType = QuizType.EXAM
            req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now()
            req.conclusionDate = LocalDateTime.now().plusHours(2)
            req.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("quiz-read")
            def found = quizService.getQuizById(quiz.aggregateId, uowR)
            found.title == "Midterm Quiz"
    }

    def "quiz titleNotBlank rejects empty title"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def uow = unitOfWorkService.createUnitOfWork("quiz-inv")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            def req = new CreateQuizRequestDto()
            req.execution = execRef
            req.questions = [] as Set
            req.title = ""
            req.quizType = QuizType.EXAM
            req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now()
            req.conclusionDate = LocalDateTime.now().plusHours(2)
            req.resultsDate = LocalDateTime.now().plusDays(1)
        when:
            quizService.createQuiz(req, uow)
        then:
            thrown(Exception)
    }

    def "updateQuiz mutates the title"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course, "Trees-quiz")
            def question = seedQuestion(course, topic, "Is BST balanced?")
            def uow1 = unitOfWorkService.createUnitOfWork("quiz-upd-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
            def qRef = new QuestionDto()
            qRef.aggregateId = question.aggregateId
            qRef.title = question.title
            def req = new CreateQuizRequestDto()
            req.execution = execRef
            req.questions = [qRef] as Set
            req.title = "Original Title"
            req.quizType = QuizType.TEST
            req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now()
            req.conclusionDate = LocalDateTime.now().plusHours(2)
            req.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("quiz-upd-get")
            def existing = quizService.getQuizById(quiz.aggregateId, uow2)
            existing.title = "Updated Title"
            def uow3 = unitOfWorkService.createUnitOfWork("quiz-upd-put")
            quizService.updateQuiz(existing, uow3)
            unitOfWorkService.commit(uow3)
        then:
            def uow4 = unitOfWorkService.createUnitOfWork("quiz-upd-read")
            def reloaded = quizService.getQuizById(quiz.aggregateId, uow4)
            reloaded.title == "Updated Title"
    }

    def "deleteQuiz succeeds"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course, "Hash-quiz")
            def question = seedQuestion(course, topic, "Is hashing O(1)?")
            def uow1 = unitOfWorkService.createUnitOfWork("quiz-del-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
            def qRef = new QuestionDto()
            qRef.aggregateId = question.aggregateId
            qRef.title = question.title
            def req = new CreateQuizRequestDto()
            req.execution = execRef
            req.questions = [qRef] as Set
            req.title = "Delete Me"
            req.quizType = QuizType.TEST
            req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now()
            req.conclusionDate = LocalDateTime.now().plusHours(2)
            req.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("quiz-del")
            quizService.deleteQuiz(quiz.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
    }
}
