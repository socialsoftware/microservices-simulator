package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserInvariantsSpec extends Specification {

    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private CreateUserRequestDto base(String username, String email) {
        def req = new CreateUserRequestDto()
        req.username = username
        req.email = email
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        return req
    }

    def "usernameNotEmpty rejects empty username at create time"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("uinv-name")
        when:
            userService.createUser(base("", "x@x.com"), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Username cannot be empty")
    }

    def "emailNotEmpty rejects empty email at create time"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("uinv-email")
        when:
            userService.createUser(base("uinv-noemail", ""), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Email cannot be empty")
    }

    def "usernameNotEmpty fires when an update would clear the username"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("uinv-up-create")
            def created = userService.createUser(base("uinv-up", "up@x.com"), uow)
            unitOfWorkService.commit(uow)
        when:
            def uowR = unitOfWorkService.createUnitOfWork("uinv-up-read")
            def existing = userService.getUserById(created.aggregateId, uowR)
            existing.username = ""
            def uowU = unitOfWorkService.createUnitOfWork("uinv-up-update")
            userService.updateUser(existing, uowU)
            unitOfWorkService.commit(uowU)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Username cannot be empty")
    }

    def "emailNotEmpty fires when an update would clear the email"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("uinv-up2-create")
            def created = userService.createUser(base("uinv-up2", "up2@x.com"), uow)
            unitOfWorkService.commit(uow)
        when:
            def uowR = unitOfWorkService.createUnitOfWork("uinv-up2-read")
            def existing = userService.getUserById(created.aggregateId, uowR)
            existing.email = ""
            def uowU = unitOfWorkService.createUnitOfWork("uinv-up2-update")
            userService.updateUser(existing, uowU)
            unitOfWorkService.commit(uowU)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Email cannot be empty")
    }
}
