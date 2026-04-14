package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
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
class OrderCrudSpec extends Specification {

    @Autowired OrderService orderService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ocrud-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "ocrud-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private CreateOrderRequestDto orderFor(UserDto u, double total, int items) {
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def req = new CreateOrderRequestDto()
        req.user = userRef
        req.totalInCents = total
        req.itemCount = items
        req.status = OrderStatus.PENDING
        req.placedAt = "2026-04-07"
        return req
    }

    def "createOrder persists with the OrderUser projection populated"() {
        given:
            def u = seedUser("create")
        when:
            def uow = unitOfWorkService.createUnitOfWork("ocrud-create")
            def order = orderService.createOrder(orderFor(u, 500.0d, 2), uow)
            unitOfWorkService.commit(uow)
        then:
            order != null
            order.user != null
            order.user.username == u.username
            order.totalInCents == 500.0d
            order.itemCount == 2
    }

    def "getAllOrders returns the order"() {
        given:
            def u = seedUser("list")
            def uow1 = unitOfWorkService.createUnitOfWork("ocrud-list-c")
            def order = orderService.createOrder(orderFor(u, 1.0d, 1), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ocrud-list-r")
            def all = orderService.getAllOrders(uow2)
        then:
            all.any { it.aggregateId == order.aggregateId }
    }

    def "updateOrder mutates total and itemCount"() {
        given:
            def u = seedUser("upd")
            def uow1 = unitOfWorkService.createUnitOfWork("ocrud-upd-c")
            def order = orderService.createOrder(orderFor(u, 100.0d, 1), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ocrud-upd-g")
            def existing = orderService.getOrderById(order.aggregateId, uow2)
            existing.totalInCents = 999.0d
            existing.itemCount = 9
            def uow3 = unitOfWorkService.createUnitOfWork("ocrud-upd-p")
            orderService.updateOrder(existing, uow3)
            unitOfWorkService.commit(uow3)
        then:
            def uow4 = unitOfWorkService.createUnitOfWork("ocrud-upd-r")
            def reloaded = orderService.getOrderById(order.aggregateId, uow4)
            reloaded.totalInCents == 999.0d
            reloaded.itemCount == 9
    }

    def "deleteOrder hides the order from later reads"() {
        given:
            def u = seedUser("del")
            def uow1 = unitOfWorkService.createUnitOfWork("ocrud-del-c")
            def order = orderService.createOrder(orderFor(u, 1.0d, 1), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ocrud-del-d")
            orderService.deleteOrder(order.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            def uow3 = unitOfWorkService.createUnitOfWork("ocrud-del-r")
            try {
                orderService.getOrderById(order.aggregateId, uow3)
                assert false
            } catch (Exception ignored) {
            }
    }
}
