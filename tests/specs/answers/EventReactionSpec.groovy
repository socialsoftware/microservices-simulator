package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.eventProcessing.TopicEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.eventProcessing.AnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserDeletedEvent
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserUpdatedEvent
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class EventReactionSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired AnswerService answerService
    @Autowired UnitOfWorkService unitOfWorkService

    @Autowired ExecutionEventProcessing executionEventProcessing
    @Autowired TopicEventProcessing topicEventProcessing
    @Autowired QuestionEventProcessing questionEventProcessing
    @Autowired AnswerEventProcessing answerEventProcessing

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("er-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "ER-${System.nanoTime()}"; req.type = CourseType.TECNICO; req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("er-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"; req.username = "er-$tag-${System.nanoTime()}"; req.role = UserRole.STUDENT; req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course, UserDto... users) {
        def uow = unitOfWorkService.createUnitOfWork("er-seed-exec")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def userRefs = users.collect { u ->
            def ref = new UserDto(); ref.aggregateId = u.aggregateId; ref.name = u.name; ref.username = u.username; return ref
        } as Set
        def req = new CreateExecutionRequestDto()
        req.course = courseRef; req.users = userRefs; req.acronym = "ER-${System.nanoTime()}"
        req.academicTerm = "2025/2026"; req.endDate = LocalDateTime.now().plusMonths(6)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("er-seed-topic")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def req = new CreateTopicRequestDto(); req.course = courseRef; req.name = "ERTopic-${System.nanoTime()}"
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "CourseDeletedEvent makes execution INACTIVE via event processing"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
        when:
            executionEventProcessing.processCourseDeletedEvent(exec.aggregateId, new CourseDeletedEvent(course.aggregateId))
        then:
            def uowR = unitOfWorkService.createUnitOfWork("er-exec-read")
            executionService.getExecutionById(exec.aggregateId, uowR).state == AggregateState.INACTIVE
    }

    def "CourseDeletedEvent makes topic INACTIVE via event processing"() {
        given:
            def course = seedCourse()
            def topic = seedTopic(course)
        when:
            topicEventProcessing.processCourseDeletedEvent(topic.aggregateId, new CourseDeletedEvent(course.aggregateId))
        then:
            def uowR = unitOfWorkService.createUnitOfWork("er-topic-read")
            topicService.getTopicById(topic.aggregateId, uowR).state == AggregateState.INACTIVE
    }

    def "CourseDeletedEvent makes question INACTIVE via event processing"() {
        given:
            def course = seedCourse()
            def topic = seedTopic(course)
            def uowQ = unitOfWorkService.createUnitOfWork("er-seed-q")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def opt = new OptionDto(); opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def qReq = new CreateQuestionRequestDto()
            qReq.course = courseRef; qReq.topics = [topicRef] as Set; qReq.title = "ER-Q-${System.nanoTime()}"
            qReq.content = "content"; qReq.creationDate = LocalDateTime.now(); qReq.options = [opt]
            def question = questionService.createQuestion(qReq, uowQ)
            unitOfWorkService.commit(uowQ)
        when:
            questionEventProcessing.processCourseDeletedEvent(question.aggregateId, new CourseDeletedEvent(course.aggregateId))
        then:
            def uowR = unitOfWorkService.createUnitOfWork("er-q-read")
            questionService.getQuestionById(question.aggregateId, uowR).state == AggregateState.INACTIVE
    }

    def "UserUpdatedEvent propagates to execution via event processing"() {
        given:
            def course = seedCourse()
            def user = seedUser("proj-sync")
            def exec = seedExecution(course, user)
        when:
            def event = new UserUpdatedEvent(user.aggregateId, "UpdatedName", user.username, user.active)
            event.setPublisherAggregateVersion(1)
            executionEventProcessing.processUserUpdatedEvent(exec.aggregateId, event)
        then:
            noExceptionThrown()
    }

    def "UserDeletedEvent makes answer INACTIVE via event processing"() {
        given:
            def course = seedCourse()
            def user = seedUser("ans-del")
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def uowQ = unitOfWorkService.createUnitOfWork("er-seed-q2")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def opt = new OptionDto(); opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def qReq = new CreateQuestionRequestDto()
            qReq.course = courseRef; qReq.topics = [topicRef] as Set; qReq.title = "ER-Q2-${System.nanoTime()}"
            qReq.content = "content"; qReq.creationDate = LocalDateTime.now(); qReq.options = [opt]
            def question = questionService.createQuestion(qReq, uowQ)
            unitOfWorkService.commit(uowQ)
            def uowQz = unitOfWorkService.createUnitOfWork("er-seed-quiz")
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId; execRef.acronym = exec.acronym
            def qRef = new QuestionDto(); qRef.aggregateId = question.aggregateId; qRef.title = question.title
            def quizReq = new CreateQuizRequestDto()
            quizReq.execution = execRef; quizReq.questions = [qRef] as Set; quizReq.title = "ERQuiz-${System.nanoTime()}"
            quizReq.quizType = QuizType.TEST; quizReq.creationDate = LocalDateTime.now()
            quizReq.availableDate = LocalDateTime.now(); quizReq.conclusionDate = LocalDateTime.now().plusHours(2)
            quizReq.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(quizReq, uowQz)
            unitOfWorkService.commit(uowQz)
            def uowA = unitOfWorkService.createUnitOfWork("er-ans-create")
            def execRef2 = new ExecutionDto(); execRef2.aggregateId = exec.aggregateId
            def userRef = new UserDto(); userRef.aggregateId = user.aggregateId; userRef.name = user.name; userRef.username = user.username
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def ansReq = new CreateAnswerRequestDto()
            ansReq.execution = execRef2; ansReq.user = userRef; ansReq.quiz = quizRef; ansReq.questions = [] as Set
            ansReq.creationDate = LocalDateTime.now(); ansReq.answerDate = LocalDateTime.now(); ansReq.completed = false
            def answer = answerService.createAnswer(ansReq, uowA)
            unitOfWorkService.commit(uowA)
        when:
            answerEventProcessing.processUserDeletedEvent(answer.aggregateId, new UserDeletedEvent(user.aggregateId))
        then:
            def uowR = unitOfWorkService.createUnitOfWork("er-ans-read")
            answerService.getAnswerById(answer.aggregateId, uowR).state == AggregateState.INACTIVE
    }

    def "ExecutionDeletedEvent makes answer INACTIVE via event processing"() {
        given:
            def course = seedCourse()
            def user = seedUser("ans-exec-del")
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def uowQ = unitOfWorkService.createUnitOfWork("er-seed-q3")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def opt = new OptionDto(); opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def qReq = new CreateQuestionRequestDto()
            qReq.course = courseRef; qReq.topics = [topicRef] as Set; qReq.title = "ER-Q3-${System.nanoTime()}"
            qReq.content = "content"; qReq.creationDate = LocalDateTime.now(); qReq.options = [opt]
            def question = questionService.createQuestion(qReq, uowQ)
            unitOfWorkService.commit(uowQ)
            def uowQz = unitOfWorkService.createUnitOfWork("er-seed-quiz2")
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId; execRef.acronym = exec.acronym
            def qRef = new QuestionDto(); qRef.aggregateId = question.aggregateId; qRef.title = question.title
            def quizReq = new CreateQuizRequestDto()
            quizReq.execution = execRef; quizReq.questions = [qRef] as Set; quizReq.title = "ERQuiz2-${System.nanoTime()}"
            quizReq.quizType = QuizType.TEST; quizReq.creationDate = LocalDateTime.now()
            quizReq.availableDate = LocalDateTime.now(); quizReq.conclusionDate = LocalDateTime.now().plusHours(2)
            quizReq.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(quizReq, uowQz)
            unitOfWorkService.commit(uowQz)
            def uowA = unitOfWorkService.createUnitOfWork("er-ans-exec-create")
            def execRef2 = new ExecutionDto(); execRef2.aggregateId = exec.aggregateId
            def userRef = new UserDto(); userRef.aggregateId = user.aggregateId; userRef.name = user.name; userRef.username = user.username
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def ansReq = new CreateAnswerRequestDto()
            ansReq.execution = execRef2; ansReq.user = userRef; ansReq.quiz = quizRef; ansReq.questions = [] as Set
            ansReq.creationDate = LocalDateTime.now(); ansReq.answerDate = LocalDateTime.now(); ansReq.completed = false
            def answer = answerService.createAnswer(ansReq, uowA)
            unitOfWorkService.commit(uowA)
        when:
            answerEventProcessing.processExecutionDeletedEvent(answer.aggregateId, new ExecutionDeletedEvent(exec.aggregateId))
        then:
            def uowR = unitOfWorkService.createUnitOfWork("er-ans-exec-read")
            answerService.getAnswerById(answer.aggregateId, uowR).state == AggregateState.INACTIVE
    }
}
