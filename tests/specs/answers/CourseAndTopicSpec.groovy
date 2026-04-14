package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CourseAndTopicSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired TopicService topicService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createCourse(String name) {
        def uow = unitOfWorkService.createUnitOfWork("course-create-$name")
        def req = new CreateCourseRequestDto()
        req.name = name
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def createTopic(CourseDto course, String name) {
        def uow = unitOfWorkService.createUnitOfWork("topic-create-$name")
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

    def "createCourse persists and reads back"() {
        when:
            def course = createCourse("Software Engineering")
        then:
            def uow = unitOfWorkService.createUnitOfWork("course-read")
            def found = courseService.getCourseById(course.aggregateId, uow)
            found.name == "Software Engineering"
    }

    def "course nameNotBlank rejects empty name"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("course-inv")
            def req = new CreateCourseRequestDto()
            req.name = ""
            req.type = CourseType.TECNICO
            req.creationDate = LocalDateTime.now()
        when:
            courseService.createCourse(req, uow)
        then:
            thrown(Exception)
    }

    def "createTopic with a course projection persists and reads back"() {
        given:
            def course = createCourse("Algorithms")
        when:
            def topic = createTopic(course, "Sorting")
        then:
            def uow = unitOfWorkService.createUnitOfWork("topic-read")
            def found = topicService.getTopicById(topic.aggregateId, uow)
            found.name == "Sorting"
    }

    def "topic nameNotBlank rejects empty name"() {
        given:
            def course = createCourse("Networks")
            def uow = unitOfWorkService.createUnitOfWork("topic-inv")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateTopicRequestDto()
            req.course = courseRef
            req.name = ""
        when:
            topicService.createTopic(req, uow)
        then:
            thrown(Exception)
    }

    def "deleteCourse succeeds (reactive model)"() {
        given:
            def course = createCourse("ToDelete")
        when:
            def uow = unitOfWorkService.createUnitOfWork("course-del")
            courseService.deleteCourse(course.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            noExceptionThrown()
    }
}
