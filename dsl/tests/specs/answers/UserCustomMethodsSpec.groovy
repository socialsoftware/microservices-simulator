package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserCustomMethodsSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createUser(String name, String username, UserRole role) {
        def uow = unitOfWorkService.createUnitOfWork("ucm-create-$username")
        def req = new CreateUserRequestDto()
        req.name = name
        req.username = username
        req.role = role
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    // --- activateUser / deactivateUser ---

    def "deactivateUser sets active to false"() {
        given:
            def user = createUser("Deact", "deact-${System.nanoTime()}", UserRole.STUDENT)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ucm-deactivate")
            userService.deactivateUser(user.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ucm-deact-read")
            def found = userService.getUserById(user.aggregateId, uowR)
            found.active == false
    }

    def "activateUser sets active back to true"() {
        given:
            def user = createUser("React", "react-${System.nanoTime()}", UserRole.STUDENT)
            def uowD = unitOfWorkService.createUnitOfWork("ucm-deact-first")
            userService.deactivateUser(user.aggregateId, uowD)
            unitOfWorkService.commit(uowD)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ucm-activate")
            userService.activateUser(user.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ucm-act-read")
            def found = userService.getUserById(user.aggregateId, uowR)
            found.active == true
    }

    // --- anonymizeUser ---

    def "anonymizeUser replaces name and username with ANONYMOUS"() {
        given:
            def user = createUser("RealName", "realuser-${System.nanoTime()}", UserRole.STUDENT)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ucm-anonymize")
            userService.anonymizeUser(user.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ucm-anon-read")
            def found = userService.getUserById(user.aggregateId, uowR)
            found.name == "ANONYMOUS"
            found.username == "ANONYMOUS"
    }

    // --- getStudents / getTeachers ---

    def "getStudents returns only students"() {
        given:
            def student = createUser("Stu", "stu-${System.nanoTime()}", UserRole.STUDENT)
            def teacher = createUser("Teach", "teach-${System.nanoTime()}", UserRole.TEACHER)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ucm-students")
            def students = userService.getStudents(uow)
        then:
            students.any { it.aggregateId == student.aggregateId }
            !students.any { it.aggregateId == teacher.aggregateId }
    }

    def "getTeachers returns only teachers"() {
        given:
            def student = createUser("Stu2", "stu2-${System.nanoTime()}", UserRole.STUDENT)
            def teacher = createUser("Teach2", "teach2-${System.nanoTime()}", UserRole.TEACHER)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ucm-teachers")
            def teachers = userService.getTeachers(uow)
        then:
            teachers.any { it.aggregateId == teacher.aggregateId }
            !teachers.any { it.aggregateId == student.aggregateId }
    }
}
