package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.service.CartService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class UserReferenceIntegritySpec extends Specification {

    @Autowired UserService userService
    @Autowired CartService cartService
    @Autowired UnitOfWorkService unitOfWorkService

    def "deleteUser succeeds even when a Cart references the user (reactive model)"() {
        given: "a user with a cart"
            def uowU = unitOfWorkService.createUnitOfWork("ref-create-user")
            def userReq = new CreateUserRequestDto()
            userReq.username = "carol"
            userReq.email = "carol@example.com"
            userReq.passwordHash = "h"
            userReq.shippingAddress = "1 Ref Street"
            def user = userService.createUser(userReq, uowU)
            unitOfWorkService.commit(uowU)

            def uowC = unitOfWorkService.createUnitOfWork("ref-create-cart")
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.username = user.username
            userRef.email = user.email
            userRef.shippingAddress = user.shippingAddress
            def cartReq = new CreateCartRequestDto()
            cartReq.user = userRef
            cartReq.totalInCents = 0.0d
            cartReq.itemCount = 0
            cartReq.checkedOut = false
            cartService.createCart(cartReq, uowC)
            unitOfWorkService.commit(uowC)

        when: "we try to delete the user"
            def uowD = unitOfWorkService.createUnitOfWork("ref-delete-user")
            userService.deleteUser(user.aggregateId, uowD)
            unitOfWorkService.commit(uowD)

        then: "the deletion succeeds (reactive model — no synchronous blocking)"
            noExceptionThrown()
    }

    def "deleteUser succeeds when no aggregate references the user"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("ref-create-orphan")
            def userReq = new CreateUserRequestDto()
            userReq.username = "dave"
            userReq.email = "dave@example.com"
            userReq.passwordHash = "h"
            userReq.shippingAddress = "addr"
            def user = userService.createUser(userReq, uow)
            unitOfWorkService.commit(uow)

        when:
            def uowD = unitOfWorkService.createUnitOfWork("ref-delete-orphan")
            userService.deleteUser(user.aggregateId, uowD)
            unitOfWorkService.commit(uowD)

        then:
            noExceptionThrown()
    }
}
