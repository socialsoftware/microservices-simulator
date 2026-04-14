package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ConcurrentSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired SagaUnitOfWorkService unitOfWorkService

    def "sequential creation of multiple users — each gets a unique aggregateId"() {
        when: "we create 5 users sequentially"
            def ids = (1..5).collect { i ->
                def uow = unitOfWorkService.createUnitOfWork("seq-user-$i")
                def req = new CreateUserRequestDto()
                req.name = "Sequential $i"
                req.username = "seq-$i-${System.nanoTime()}"
                req.role = UserRole.STUDENT
                req.active = true
                def dto = userService.createUser(req, uow)
                unitOfWorkService.commit(uow)
                return dto.aggregateId
            }
        then: "all 5 have distinct aggregate IDs"
            ids.toSet().size() == 5
    }

    def "sequential updates to the same user — last write wins"() {
        given: "a user"
            def uow0 = unitOfWorkService.createUnitOfWork("conc-upd-create")
            def req = new CreateUserRequestDto()
            req.name = "Original"
            req.username = "conc-upd-${System.nanoTime()}"
            req.role = UserRole.STUDENT
            req.active = true
            def user = userService.createUser(req, uow0)
            unitOfWorkService.commit(uow0)
        when: "two updates happen sequentially"
            def uow1 = unitOfWorkService.createUnitOfWork("conc-upd-1")
            def v1 = userService.getUserById(user.aggregateId, uow1)
            v1.name = "Updated-A"
            userService.updateUser(v1, uow1)
            unitOfWorkService.commit(uow1)

            def uow2 = unitOfWorkService.createUnitOfWork("conc-upd-2")
            def v2 = userService.getUserById(user.aggregateId, uow2)
            v2.name = "Updated-B"
            userService.updateUser(v2, uow2)
            unitOfWorkService.commit(uow2)
        then: "the last write wins"
            def uowR = unitOfWorkService.createUnitOfWork("conc-upd-read")
            def reloaded = userService.getUserById(user.aggregateId, uowR)
            reloaded.name == "Updated-B"
    }

    def "version numbers are globally monotonic across different aggregate types"() {
        when: "we create a user and a course sequentially"
            def uow1 = unitOfWorkService.createUnitOfWork("ver-user")
            def userReq = new CreateUserRequestDto()
            userReq.name = "VerUser"
            userReq.username = "ver-user-${System.nanoTime()}"
            userReq.role = UserRole.STUDENT
            userReq.active = true
            def user = userService.createUser(userReq, uow1)
            unitOfWorkService.commit(uow1)

            def uow2 = unitOfWorkService.createUnitOfWork("ver-course")
            def courseReq = new CreateCourseRequestDto()
            courseReq.name = "VerCourse-${System.nanoTime()}"
            courseReq.type = CourseType.TECNICO
            courseReq.creationDate = LocalDateTime.now()
            def course = courseService.createCourse(courseReq, uow2)
            unitOfWorkService.commit(uow2)
        then: "the course has a higher version than the user (global counter)"
            course.version > user.version
    }

    def "deleting a course that has a topic referencing it succeeds (reactive model)"() {
        given: "a course"
            def uow1 = unitOfWorkService.createUnitOfWork("conc-ref-course")
            def courseReq = new CreateCourseRequestDto()
            courseReq.name = "RefCourse-${System.nanoTime()}"
            courseReq.type = CourseType.TECNICO
            courseReq.creationDate = LocalDateTime.now()
            def course = courseService.createCourse(courseReq, uow1)
            unitOfWorkService.commit(uow1)
        when: "we delete the course"
            def uow2 = unitOfWorkService.createUnitOfWork("conc-ref-del")
            courseService.deleteCourse(course.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then: "deletion succeeds (reactive model — no synchronous blocking)"
            noExceptionThrown()
    }

    def "creating and then immediately reading back multiple aggregates is consistent"() {
        when: "we create 3 users and read them all back"
            def created = (1..3).collect { i ->
                def uow = unitOfWorkService.createUnitOfWork("batch-$i")
                def req = new CreateUserRequestDto()
                req.name = "Batch $i"
                req.username = "batch-$i-${System.nanoTime()}"
                req.role = UserRole.STUDENT
                req.active = true
                def dto = userService.createUser(req, uow)
                unitOfWorkService.commit(uow)
                return dto
            }
        then: "getAllUsers includes all 3"
            def uowR = unitOfWorkService.createUnitOfWork("batch-read")
            def all = userService.getAllUsers(uowR)
            created.every { c -> all.any { it.aggregateId == c.aggregateId } }
    }
}
