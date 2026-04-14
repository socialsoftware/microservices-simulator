package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.service.CartService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CartInvariantsSpec extends Specification {

    @Autowired CartService cartService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("cinv-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "cinv-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private CreateCartRequestDto cartFor(UserDto u, double total, int count) {
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def req = new CreateCartRequestDto()
        req.user = userRef
        req.totalInCents = total
        req.itemCount = count
        req.checkedOut = false
        return req
    }

    def "totalNonNegative rejects negative total at create time"() {
        given:
            def u = seedUser("negt")
            def uow = unitOfWorkService.createUnitOfWork("cinv-negt")
        when:
            cartService.createCart(cartFor(u, -1.0d, 0), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Cart total cannot be negative")
    }

    def "itemCountNonNegative rejects negative item count at create time"() {
        given:
            def u = seedUser("negi")
            def uow = unitOfWorkService.createUnitOfWork("cinv-negi")
        when:
            cartService.createCart(cartFor(u, 0.0d, -1), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Cart item count cannot be negative")
    }

    def "zero total and zero items are accepted (boundary inclusive)"() {
        given:
            def u = seedUser("zero")
            def uow = unitOfWorkService.createUnitOfWork("cinv-zero")
        when:
            def cart = cartService.createCart(cartFor(u, 0.0d, 0), uow)
            unitOfWorkService.commit(uow)
        then:
            cart.totalInCents == 0.0d
            cart.itemCount == 0
    }
}
