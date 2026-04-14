package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserCrudSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private Integer createUser(String username, String email) {
        def uow = unitOfWorkService.createUnitOfWork("user-create-$username")
        def req = new CreateUserRequestDto()
        req.username = username
        req.email = email
        req.passwordHash = "h"
        req.shippingAddress = "1 Some Street"
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto.aggregateId
    }

    def "createUser persists a row that getUserById can read back"() {
        when:
            def id = createUser("ucrud-alice", "alice@ucrud.com")
        then:
            def uow = unitOfWorkService.createUnitOfWork("user-read")
            def found = userService.getUserById(id, uow)
            found.username == "ucrud-alice"
            found.email == "alice@ucrud.com"
    }

    def "getAllUsers returns every non-deleted row"() {
        given:
            createUser("ucrud-listA", "lista@x.com")
            createUser("ucrud-listB", "listb@x.com")
        when:
            def uow = unitOfWorkService.createUnitOfWork("user-list")
            def all = userService.getAllUsers(uow)
        then:
            all.any { it.username == "ucrud-listA" }
            all.any { it.username == "ucrud-listB" }
    }

    def "updateUser mutates the persisted row"() {
        given:
            def id = createUser("ucrud-up", "up@x.com")
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("user-up-get")
            def existing = userService.getUserById(id, uowGet)
            existing.email = "renamed@x.com"
            existing.shippingAddress = "999 New Place"
            def uowPut = unitOfWorkService.createUnitOfWork("user-up-put")
            userService.updateUser(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-up-read")
            def reloaded = userService.getUserById(id, uowR)
            reloaded.email == "renamed@x.com"
            reloaded.shippingAddress == "999 New Place"
    }

    def "deleteUser hides the row from subsequent reads"() {
        given:
            def id = createUser("ucrud-del", "del@x.com")
        when:
            def uowD = unitOfWorkService.createUnitOfWork("user-del")
            userService.deleteUser(id, uowD)
            unitOfWorkService.commit(uowD)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("user-del-read")
            try {
                userService.getUserById(id, uowR)
                assert false : "expected getUserById to throw or return DELETED"
            } catch (Exception ignored) {
            }
    }

    def "makeUser custom method creates a user via the action sublanguage"() {
        when:
            def uow = unitOfWorkService.createUnitOfWork("user-makeuser")
            def dto = userService.makeUser("ucrud-make", "make@x.com", "addr", uow)
            unitOfWorkService.commit(uow)
        then:
            dto != null
            dto.username == "ucrud-make"
            dto.email == "make@x.com"
            dto.shippingAddress == "addr"
    }
}
