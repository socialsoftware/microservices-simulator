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
class CartCrudSpec extends Specification {

    @Autowired CartService cartService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ccrud-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "ccrud-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private CreateCartRequestDto cartFor(UserDto u, double total, int count, boolean checkedOut) {
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def req = new CreateCartRequestDto()
        req.user = userRef
        req.totalInCents = total
        req.itemCount = count
        req.checkedOut = checkedOut
        return req
    }

    def "createCart persists with a CartUser projection"() {
        given:
            def u = seedUser("create")
        when:
            def uow = unitOfWorkService.createUnitOfWork("ccrud-create")
            def cart = cartService.createCart(cartFor(u, 0.0d, 0, false), uow)
            unitOfWorkService.commit(uow)
        then:
            cart != null
            cart.user != null
            cart.user.username == u.username
    }

    def "getCartById reads back a created cart"() {
        given:
            def u = seedUser("read")
            def uow1 = unitOfWorkService.createUnitOfWork("ccrud-read-c")
            def cart = cartService.createCart(cartFor(u, 200.0d, 2, false), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ccrud-read-r")
            def found = cartService.getCartById(cart.aggregateId, uow2)
        then:
            found.totalInCents == 200.0d
            found.itemCount == 2
            !found.checkedOut
    }

    def "updateCart mutates totals and checkedOut"() {
        given:
            def u = seedUser("upd")
            def uow1 = unitOfWorkService.createUnitOfWork("ccrud-upd-c")
            def cart = cartService.createCart(cartFor(u, 50.0d, 1, false), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ccrud-upd-g")
            def existing = cartService.getCartById(cart.aggregateId, uow2)
            existing.totalInCents = 175.0d
            existing.itemCount = 4
            existing.checkedOut = true
            def uow3 = unitOfWorkService.createUnitOfWork("ccrud-upd-p")
            cartService.updateCart(existing, uow3)
            unitOfWorkService.commit(uow3)
        then:
            def uow4 = unitOfWorkService.createUnitOfWork("ccrud-upd-r")
            def reloaded = cartService.getCartById(cart.aggregateId, uow4)
            reloaded.totalInCents == 175.0d
            reloaded.itemCount == 4
            reloaded.checkedOut
    }

    def "deleteCart hides the cart from later reads"() {
        given:
            def u = seedUser("del")
            def uow1 = unitOfWorkService.createUnitOfWork("ccrud-del-c")
            def cart = cartService.createCart(cartFor(u, 0.0d, 0, false), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ccrud-del-d")
            cartService.deleteCart(cart.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            def uow3 = unitOfWorkService.createUnitOfWork("ccrud-del-r")
            try {
                cartService.getCartById(cart.aggregateId, uow3)
                assert false
            } catch (Exception ignored) {
            }
    }
}
