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
class OrderStateMachineSpec extends Specification {

    @Autowired OrderService orderService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("osm-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "osm-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private Integer seedPendingOrder(UserDto u, String tag) {
        def uow = unitOfWorkService.createUnitOfWork("osm-order-$tag")
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def req = new CreateOrderRequestDto()
        req.user = userRef
        req.totalInCents = 1000.0d
        req.itemCount = 1
        req.status = OrderStatus.PENDING
        req.placedAt = "2026-04-08"
        def order = orderService.createOrder(req, uow)
        unitOfWorkService.commit(uow)
        return order.aggregateId
    }

    def "PENDING -> PAID is allowed and the persisted order shows PAID"() {
        given:
            def u = seedUser("happy")
            def id = seedPendingOrder(u, "happy")
        when:
            def uow = unitOfWorkService.createUnitOfWork("osm-mp")
            orderService.markPaid(id, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("osm-mp-r")
            def reloaded = orderService.getOrderById(id, uowR)
            reloaded.status == "PAID"
    }

    def "PAID -> SHIPPED -> DELIVERED is the happy path"() {
        given:
            def u = seedUser("chain")
            def id = seedPendingOrder(u, "chain")
        when:
            def uow1 = unitOfWorkService.createUnitOfWork("osm-chain-1")
            orderService.markPaid(id, uow1)
            unitOfWorkService.commit(uow1)
            def uow2 = unitOfWorkService.createUnitOfWork("osm-chain-2")
            orderService.markShipped(id, uow2)
            unitOfWorkService.commit(uow2)
            def uow3 = unitOfWorkService.createUnitOfWork("osm-chain-3")
            orderService.markDelivered(id, uow3)
            unitOfWorkService.commit(uow3)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("osm-chain-r")
            orderService.getOrderById(id, uowR).status == "DELIVERED"
    }

    def "PENDING -> SHIPPED is rejected (no direct transition)"() {
        given:
            def u = seedUser("skip")
            def id = seedPendingOrder(u, "skip")
        when:
            def uow = unitOfWorkService.createUnitOfWork("osm-skip")
            orderService.markShipped(id, uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Invalid state transition")
    }

    def "any transition out of DELIVERED is rejected (terminal state)"() {
        given:
            def u = seedUser("terminal")
            def id = seedPendingOrder(u, "terminal")
            // walk to DELIVERED
            def uow1 = unitOfWorkService.createUnitOfWork("osm-term-1")
            orderService.markPaid(id, uow1)
            unitOfWorkService.commit(uow1)
            def uow2 = unitOfWorkService.createUnitOfWork("osm-term-2")
            orderService.markShipped(id, uow2)
            unitOfWorkService.commit(uow2)
            def uow3 = unitOfWorkService.createUnitOfWork("osm-term-3")
            orderService.markDelivered(id, uow3)
            unitOfWorkService.commit(uow3)
        when:
            def uow4 = unitOfWorkService.createUnitOfWork("osm-term-4")
            orderService.cancelOrder(id, uow4)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("terminal state")
    }

    def "marking PAID twice in a row is idempotent (self-transition is allowed)"() {
        given:
            def u = seedUser("idem")
            def id = seedPendingOrder(u, "idem")
        when:
            def uow1 = unitOfWorkService.createUnitOfWork("osm-idem-1")
            orderService.markPaid(id, uow1)
            unitOfWorkService.commit(uow1)
            def uow2 = unitOfWorkService.createUnitOfWork("osm-idem-2")
            orderService.markPaid(id, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
            def uowR = unitOfWorkService.createUnitOfWork("osm-idem-r")
            orderService.getOrderById(id, uowR).status == "PAID"
    }
}
