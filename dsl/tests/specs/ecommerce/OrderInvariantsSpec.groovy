package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.service.OrderService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.OrderStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class OrderInvariantsSpec extends Specification {

    @Autowired OrderService orderService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("oinv-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "oinv-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private CreateOrderRequestDto orderFor(UserDto u, double total, int count) {
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def req = new CreateOrderRequestDto()
        req.user = userRef
        req.totalInCents = total
        req.itemCount = count
        req.status = OrderStatus.PENDING
        req.placedAt = "2026-04-07"
        return req
    }

    def "totalPositive rejects zero total"() {
        given:
            def u = seedUser("zerot")
            def uow = unitOfWorkService.createUnitOfWork("oinv-zero-total")
        when:
            orderService.createOrder(orderFor(u, 0.0d, 1), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Order total must be positive")
    }

    def "totalPositive rejects negative total"() {
        given:
            def u = seedUser("negt")
            def uow = unitOfWorkService.createUnitOfWork("oinv-neg-total")
        when:
            orderService.createOrder(orderFor(u, -50.0d, 1), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Order total must be positive")
    }

    def "hasItems rejects zero itemCount"() {
        given:
            def u = seedUser("zeroi")
            def uow = unitOfWorkService.createUnitOfWork("oinv-zero-items")
        when:
            orderService.createOrder(orderFor(u, 100.0d, 0), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Order must contain at least one item")
    }

    def "hasItems rejects negative itemCount"() {
        given:
            def u = seedUser("negi")
            def uow = unitOfWorkService.createUnitOfWork("oinv-neg-items")
        when:
            orderService.createOrder(orderFor(u, 100.0d, -3), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Order must contain at least one item")
    }
}
