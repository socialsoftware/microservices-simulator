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
class DeleteReactionSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired TopicService topicService
    @Autowired ExecutionService executionService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("delr-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "DelReact-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "deleting a course that has topics referencing it succeeds — topic reacts via interInvariant"() {
        given: "a course with a topic"
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delr-topic")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def topicReq = new CreateTopicRequestDto()
            topicReq.course = courseRef
            topicReq.name = "WillOrphan-${System.nanoTime()}"
            topicService.createTopic(topicReq, uow1)
            unitOfWorkService.commit(uow1)
        when: "we delete the course"
            def uow2 = unitOfWorkService.createUnitOfWork("delr-course-del")
            courseService.deleteCourse(course.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then: "the deletion succeeds — subscribers react asynchronously"
            noExceptionThrown()
    }

    def "deleting a course that has an execution referencing it succeeds — execution reacts via interInvariant"() {
        given: "a course with an execution"
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delr-exec")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef
            execReq.users = [] as Set
            execReq.acronym = "DELR-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            executionService.createExecution(execReq, uow1)
            unitOfWorkService.commit(uow1)
        when: "we delete the course"
            def uow2 = unitOfWorkService.createUnitOfWork("delr-course-del2")
            courseService.deleteCourse(course.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then: "the deletion succeeds — subscribers react asynchronously"
            noExceptionThrown()
    }

    def "deleting a user that is enrolled in an execution succeeds (reactive model)"() {
        given: "a user enrolled in an execution"
            def course = seedCourse()
            def uow1 = unitOfWorkService.createUnitOfWork("delr-user")
            def userReq = new CreateUserRequestDto()
            userReq.name = "DelUser"
            userReq.username = "delr-user-${System.nanoTime()}"
            userReq.role = UserRole.STUDENT
            userReq.active = true
            def user = userService.createUser(userReq, uow1)
            unitOfWorkService.commit(uow1)

            def uow2 = unitOfWorkService.createUnitOfWork("delr-exec2")
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
            execReq.acronym = "DELRU-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            executionService.createExecution(execReq, uow2)
            unitOfWorkService.commit(uow2)
        when: "we delete the user"
            def uow3 = unitOfWorkService.createUnitOfWork("delr-user-del")
            userService.deleteUser(user.aggregateId, uow3)
            unitOfWorkService.commit(uow3)
        then: "the deletion succeeds — execution reacts asynchronously"
            noExceptionThrown()
    }

    def "deleting a user with no references succeeds trivially"() {
        given:
            def uow1 = unitOfWorkService.createUnitOfWork("delr-orphan")
            def userReq = new CreateUserRequestDto()
            userReq.name = "Orphan"
            userReq.username = "delr-orphan-${System.nanoTime()}"
            userReq.role = UserRole.STUDENT
            userReq.active = true
            def user = userService.createUser(userReq, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("delr-orphan-del")
            userService.deleteUser(user.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
    }
}
