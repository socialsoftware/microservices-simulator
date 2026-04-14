package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserCrudSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createUser(String name, String username, UserRole role) {
        def uow = unitOfWorkService.createUnitOfWork("user-create-$username")
        def req = new CreateUserRequestDto()
        req.name = name
        req.username = username
        req.role = role
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "createUser persists and getUserById reads back"() {
        when:
            def user = createUser("Alice", "alice01", UserRole.STUDENT)
        then:
            def uow = unitOfWorkService.createUnitOfWork("user-read")
            def found = userService.getUserById(user.aggregateId, uow)
            found.name == "Alice"
            found.username == "alice01"
    }

    def "getAllUsers returns created users"() {
        given:
            createUser("Bob", "bob01", UserRole.STUDENT)
            createUser("Carol", "carol01", UserRole.TEACHER)
        when:
            def uow = unitOfWorkService.createUnitOfWork("user-list")
            def all = userService.getAllUsers(uow)
        then:
            all.any { it.username == "bob01" }
            all.any { it.username == "carol01" }
    }

    def "updateUser mutates the persisted row"() {
        given:
            def user = createUser("Dave", "dave01", UserRole.STUDENT)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("user-upd-get")
            def existing = userService.getUserById(user.aggregateId, uowGet)
            existing.name = "David"
            def uowPut = unitOfWorkService.createUnitOfWork("user-upd-put")
            userService.updateUser(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-upd-read")
            def reloaded = userService.getUserById(user.aggregateId, uowR)
            reloaded.name == "David"
    }

    def "deleteUser hides the row from subsequent reads"() {
        given:
            def user = createUser("Eve", "eve01", UserRole.STUDENT)
        when:
            def uowDel = unitOfWorkService.createUnitOfWork("user-del")
            userService.deleteUser(user.aggregateId, uowDel)
            unitOfWorkService.commit(uowDel)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-del-read")
            try {
                userService.getUserById(user.aggregateId, uowR)
                assert false : "expected to throw"
            } catch (Exception ignored) {}
    }

    def "nameNotBlank invariant rejects empty name"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("user-inv-name")
            def req = new CreateUserRequestDto()
            req.name = ""
            req.username = "inv01"
            req.role = UserRole.STUDENT
            req.active = true
        when:
            userService.createUser(req, uow)
        then:
            thrown(Exception)
    }

    def "usernameNotBlank invariant rejects empty username"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("user-inv-uname")
            def req = new CreateUserRequestDto()
            req.name = "Test"
            req.username = ""
            req.role = UserRole.STUDENT
            req.active = true
        when:
            userService.createUser(req, uow)
        then:
            thrown(Exception)
    }
}
