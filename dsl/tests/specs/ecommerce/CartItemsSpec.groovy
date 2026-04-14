package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.service.CartService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CartItemsSpec extends Specification {

    @Autowired CartService cartService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("citems-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "citems-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private CartItemDto item(long productId, int qty, double unitPrice) {
        def i = new CartItemDto()
        i.productId = productId
        i.quantity = qty
        i.unitPriceInCents = unitPrice
        return i
    }

    private CreateCartRequestDto cartFor(UserDto u, Set<CartItemDto> items, double total, int count) {
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def req = new CreateCartRequestDto()
        req.user = userRef
        req.items = items
        req.totalInCents = total
        req.itemCount = count
        req.checkedOut = false
        return req
    }

    def "createCart with a non-empty items set persists every line"() {
        given:
            def u = seedUser("multi")
            def items = [item(101L, 2, 500.0d), item(102L, 1, 1500.0d)] as Set
        when:
            def uow = unitOfWorkService.createUnitOfWork("citems-create")
            def cart = cartService.createCart(cartFor(u, items, 2500.0d, 3), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("citems-read")
            def reloaded = cartService.getCartById(cart.aggregateId, uowR)
            reloaded.items != null
            reloaded.items.size() == 2
            reloaded.items.any { it.productId == 101L && it.quantity == 2 && it.unitPriceInCents == 500.0d }
            reloaded.items.any { it.productId == 102L && it.quantity == 1 && it.unitPriceInCents == 1500.0d }
    }

    def "createCart with an empty items set persists with no lines"() {
        given:
            def u = seedUser("empty")
        when:
            def uow = unitOfWorkService.createUnitOfWork("citems-empty")
            def cart = cartService.createCart(cartFor(u, [] as Set, 0.0d, 0), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("citems-empty-r")
            def reloaded = cartService.getCartById(cart.aggregateId, uowR)
            reloaded.items == null || reloaded.items.size() == 0
    }

    def "items persist independently across multiple carts for the same user"() {
        given:
            def u = seedUser("two")
        when:
            def uow1 = unitOfWorkService.createUnitOfWork("citems-two-1")
            def cartA = cartService.createCart(cartFor(u, [item(1L, 1, 10.0d)] as Set, 10.0d, 1), uow1)
            unitOfWorkService.commit(uow1)
            def uow2 = unitOfWorkService.createUnitOfWork("citems-two-2")
            def cartB = cartService.createCart(cartFor(u, [item(2L, 5, 20.0d)] as Set, 100.0d, 5), uow2)
            unitOfWorkService.commit(uow2)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("citems-two-r")
            def aReloaded = cartService.getCartById(cartA.aggregateId, uowR)
            def bReloaded = cartService.getCartById(cartB.aggregateId, uowR)
            aReloaded.items.size() == 1
            aReloaded.items.first().productId == 1L
            bReloaded.items.size() == 1
            bReloaded.items.first().productId == 2L
    }
}
