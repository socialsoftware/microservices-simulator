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
class OrderUserProjectionSpec extends Specification {

    @Autowired
    OrderService orderService

    @Autowired
    UserService userService

    @Autowired
    UnitOfWorkService unitOfWorkService

    def "create-order snapshots the source User fields into the OrderUser projection"() {
        given: "a user with known field values"
            def uowUser = unitOfWorkService.createUnitOfWork("spec-proj-create-user")
            def userReq = new CreateUserRequestDto()
            userReq.username = "bob"
            userReq.email = "bob@example.com"
            userReq.passwordHash = "h"
            userReq.shippingAddress = "42 Snapshot Lane"
            def user = userService.createUser(userReq, uowUser)
            unitOfWorkService.commit(uowUser)

        when: "an Order is created referencing that user"
            def uowOrder = unitOfWorkService.createUnitOfWork("spec-proj-create-order")
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.username = user.username
            userRef.email = user.email
            userRef.shippingAddress = user.shippingAddress
            def req = new CreateOrderRequestDto()
            req.user = userRef
            req.totalInCents = 100.0d
            req.itemCount = 1
            req.status = OrderStatus.PENDING
            req.placedAt = "2026-04-07"
            def order = orderService.createOrder(req, uowOrder)
            unitOfWorkService.commit(uowOrder)

        then: "the persisted Order's OrderUser projection holds the snapshot"
            def uowRead = unitOfWorkService.createUnitOfWork("spec-proj-read")
            def reloaded = orderService.getOrderById(order.aggregateId, uowRead)
            reloaded.user != null
            reloaded.user.aggregateId == user.aggregateId
            reloaded.user.username == "bob"
            reloaded.user.email == "bob@example.com"
            reloaded.user.shippingAddress == "42 Snapshot Lane"
    }
}
