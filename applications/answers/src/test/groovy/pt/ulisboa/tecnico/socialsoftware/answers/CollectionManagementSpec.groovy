package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
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
class CollectionManagementSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("coll-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "SE-coll-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("coll-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "CollUser $tag"
        req.username = "coll-$tag-${System.nanoTime()}"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "execution created with multiple users has all users in the projection"() {
        given:
            def course = seedCourse()
            def userA = seedUser("a")
            def userB = seedUser("b")
            def userC = seedUser("c")
        when:
            def uow = unitOfWorkService.createUnitOfWork("coll-multi-exec")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def userRefA = new UserDto()
            userRefA.aggregateId = userA.aggregateId
            userRefA.name = userA.name
            userRefA.username = userA.username
            def userRefB = new UserDto()
            userRefB.aggregateId = userB.aggregateId
            userRefB.name = userB.name
            userRefB.username = userB.username
            def userRefC = new UserDto()
            userRefC.aggregateId = userC.aggregateId
            userRefC.name = userC.name
            userRefC.username = userC.username
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [userRefA, userRefB, userRefC] as Set
            req.acronym = "COLL-${System.nanoTime()}"
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("coll-multi-read")
            def found = executionService.getExecutionById(exec.aggregateId, uowR)
            found.users != null
            found.users.size() == 3
    }

    def "execution with empty users set persists with zero users"() {
        given:
            def course = seedCourse()
        when:
            def uow = unitOfWorkService.createUnitOfWork("coll-empty-exec")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [] as Set
            req.acronym = "EMPTY-${System.nanoTime()}"
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("coll-empty-read")
            def found = executionService.getExecutionById(exec.aggregateId, uowR)
            found.users == null || found.users.size() == 0
    }

    def "two executions with different users are independent"() {
        given:
            def course = seedCourse()
            def userA = seedUser("indA")
            def userB = seedUser("indB")
        when:
            def uow1 = unitOfWorkService.createUnitOfWork("coll-ind-1")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def refA = new UserDto()
            refA.aggregateId = userA.aggregateId
            refA.name = userA.name
            refA.username = userA.username
            def req1 = new CreateExecutionRequestDto()
            req1.course = courseRef
            req1.users = [refA] as Set
            req1.acronym = "IND-A-${System.nanoTime()}"
            req1.academicTerm = "2025/2026"
            req1.endDate = LocalDateTime.now().plusMonths(3)
            def exec1 = executionService.createExecution(req1, uow1)
            unitOfWorkService.commit(uow1)

            def uow2 = unitOfWorkService.createUnitOfWork("coll-ind-2")
            def refB = new UserDto()
            refB.aggregateId = userB.aggregateId
            refB.name = userB.name
            refB.username = userB.username
            def req2 = new CreateExecutionRequestDto()
            req2.course = courseRef
            req2.users = [refB] as Set
            req2.acronym = "IND-B-${System.nanoTime()}"
            req2.academicTerm = "2025/2026"
            req2.endDate = LocalDateTime.now().plusMonths(3)
            def exec2 = executionService.createExecution(req2, uow2)
            unitOfWorkService.commit(uow2)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("coll-ind-read")
            def found1 = executionService.getExecutionById(exec1.aggregateId, uowR)
            def found2 = executionService.getExecutionById(exec2.aggregateId, uowR)
            found1.users.size() == 1
            found2.users.size() == 1
            found1.aggregateId != found2.aggregateId
    }
}
