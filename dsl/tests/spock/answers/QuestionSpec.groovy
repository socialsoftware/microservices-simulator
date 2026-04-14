package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class QuestionSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("q-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "SE-question"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course, String name) {
        def uow = unitOfWorkService.createUnitOfWork("q-seed-topic-$name")
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

    def "createQuestion with course, topic, and options persists correctly"() {
        given:
            def course = seedCourse()
            def topic = seedTopic(course, "Graphs")
        when:
            def uow = unitOfWorkService.createUnitOfWork("q-create")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def topicRef = new TopicDto()
            topicRef.aggregateId = topic.aggregateId
            topicRef.name = topic.name
            def opt1 = new OptionDto()
            opt1.key = 1
            opt1.sequence = 1
            opt1.correct = true
            opt1.content = "Yes"
            def opt2 = new OptionDto()
            opt2.key = 2
            opt2.sequence = 2
            opt2.correct = false
            opt2.content = "No"
            def req = new CreateQuestionRequestDto()
            req.course = courseRef
            req.topics = [topicRef] as Set
            req.title = "Is DFS recursive?"
            req.content = "Can DFS be implemented recursively?"
            req.creationDate = LocalDateTime.now()
            req.options = [opt1, opt2]
            def question = questionService.createQuestion(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("q-read")
            def found = questionService.getQuestionById(question.aggregateId, uowR)
            found.title == "Is DFS recursive?"
            found.content == "Can DFS be implemented recursively?"
    }

    def "question titleNotBlank rejects empty title"() {
        given:
            def course = seedCourse()
            def uow = unitOfWorkService.createUnitOfWork("q-inv")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateQuestionRequestDto()
            req.course = courseRef
            req.topics = [] as Set
            req.title = ""
            req.content = "some content"
            req.creationDate = LocalDateTime.now()
            req.options = []
        when:
            questionService.createQuestion(req, uow)
        then:
            thrown(Exception)
    }
}
