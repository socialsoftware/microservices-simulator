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
class ExecutionSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("exec-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "SE-exec"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("exec-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "exec-$tag"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "createExecution with course and users persists correctly"() {
        given:
            def course = seedCourse()
            def user = seedUser("exec1")
        when:
            def uow = unitOfWorkService.createUnitOfWork("exec-create")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.name = user.name
            userRef.username = user.username
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [userRef] as Set
            req.acronym = "SE-2026"
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("exec-read")
            def found = executionService.getExecutionById(exec.aggregateId, uowR)
            found.acronym == "SE-2026"
            found.academicTerm == "2025/2026"
    }

    def "execution acronymNotBlank rejects empty acronym"() {
        given:
            def course = seedCourse()
            def uow = unitOfWorkService.createUnitOfWork("exec-inv")
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [] as Set
            req.acronym = ""
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
        when:
            executionService.createExecution(req, uow)
        then:
            thrown(Exception)
    }
}
