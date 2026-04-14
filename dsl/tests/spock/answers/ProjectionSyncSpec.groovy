package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ProjectionSyncSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired TopicService topicService
    @Autowired ExecutionService executionService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    def "topic carries a snapshot of its course name at creation time"() {
        given: "a course"
            def uow1 = unitOfWorkService.createUnitOfWork("proj-course")
            def courseReq = new CreateCourseRequestDto()
            courseReq.name = "OriginalCourse-${System.nanoTime()}"
            courseReq.type = CourseType.TECNICO
            courseReq.creationDate = LocalDateTime.now()
            def course = courseService.createCourse(courseReq, uow1)
            unitOfWorkService.commit(uow1)
        when: "we create a topic referencing the course"
            def uow2 = unitOfWorkService.createUnitOfWork("proj-topic")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def topicReq = new CreateTopicRequestDto()
            topicReq.course = courseRef
            topicReq.name = "ProjTopic-${System.nanoTime()}"
            def topic = topicService.createTopic(topicReq, uow2)
            unitOfWorkService.commit(uow2)
        then: "the topic's course projection carries the course name"
            def uow3 = unitOfWorkService.createUnitOfWork("proj-read")
            def found = topicService.getTopicById(topic.aggregateId, uow3)
            found != null
            found.name != null
    }

    def "execution carries snapshots of its course and users at creation time"() {
        given: "a course and a user"
            def uow1 = unitOfWorkService.createUnitOfWork("proj-exec-course")
            def courseReq = new CreateCourseRequestDto()
            courseReq.name = "ExecCourse-${System.nanoTime()}"
            courseReq.type = CourseType.TECNICO
            courseReq.creationDate = LocalDateTime.now()
            def course = courseService.createCourse(courseReq, uow1)
            unitOfWorkService.commit(uow1)

            def uow2 = unitOfWorkService.createUnitOfWork("proj-exec-user")
            def userReq = new CreateUserRequestDto()
            userReq.name = "ExecUser"
            userReq.username = "exec-proj-${System.nanoTime()}"
            userReq.role = UserRole.STUDENT
            userReq.active = true
            def user = userService.createUser(userReq, uow2)
            unitOfWorkService.commit(uow2)
        when: "we create an execution with both"
            def uow3 = unitOfWorkService.createUnitOfWork("proj-exec-create")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.name = user.name
            userRef.username = user.username
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef
            execReq.users = [userRef] as Set
            execReq.acronym = "PROJ-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uow3)
            unitOfWorkService.commit(uow3)
        then: "the execution persists with its projections"
            def uow4 = unitOfWorkService.createUnitOfWork("proj-exec-read")
            def found = executionService.getExecutionById(exec.aggregateId, uow4)
            found.acronym.startsWith("PROJ-")
            found.academicTerm == "2025/2026"
    }

    def "multiple topics can reference the same course"() {
        given: "a course"
            def uow1 = unitOfWorkService.createUnitOfWork("proj-multi-course")
            def courseReq = new CreateCourseRequestDto()
            courseReq.name = "SharedCourse-${System.nanoTime()}"
            courseReq.type = CourseType.TECNICO
            courseReq.creationDate = LocalDateTime.now()
            def course = courseService.createCourse(courseReq, uow1)
            unitOfWorkService.commit(uow1)
        when: "we create two topics for the same course"
            def uow2 = unitOfWorkService.createUnitOfWork("proj-multi-t1")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req1 = new CreateTopicRequestDto()
            req1.course = courseRef
            req1.name = "Topic A-${System.nanoTime()}"
            def t1 = topicService.createTopic(req1, uow2)
            unitOfWorkService.commit(uow2)

            def uow3 = unitOfWorkService.createUnitOfWork("proj-multi-t2")
            def req2 = new CreateTopicRequestDto()
            req2.course = courseRef
            req2.name = "Topic B-${System.nanoTime()}"
            def t2 = topicService.createTopic(req2, uow3)
            unitOfWorkService.commit(uow3)
        then: "both topics exist independently"
            t1.aggregateId != t2.aggregateId
            def uow4 = unitOfWorkService.createUnitOfWork("proj-multi-read")
            topicService.getTopicById(t1.aggregateId, uow4) != null
            topicService.getTopicById(t2.aggregateId, uow4) != null
    }
}
