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
class OrderUserProjectionSyncSpec extends Specification {

    @Autowired
    OrderService orderService

    @Autowired
    UserService userService

    @Autowired
    UnitOfWorkService unitOfWorkService

    def "updating a User refreshes the OrderUser projection on existing Orders (async)"() {
        given: "a user with an existing order"
            def uowU = unitOfWorkService.createUnitOfWork("sync-create-user")
            def userReq = new CreateUserRequestDto()
            userReq.username = "eve"
            userReq.email = "eve@example.com"
            userReq.passwordHash = "h"
            userReq.shippingAddress = "1 Old Lane"
            def user = userService.createUser(userReq, uowU)
            unitOfWorkService.commit(uowU)

            def uowO = unitOfWorkService.createUnitOfWork("sync-create-order")
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.username = user.username
            userRef.email = user.email
            userRef.shippingAddress = user.shippingAddress
            def orderReq = new CreateOrderRequestDto()
            orderReq.user = userRef
            orderReq.totalInCents = 100.0d
            orderReq.itemCount = 1
            orderReq.status = OrderStatus.PENDING
            orderReq.placedAt = "2026-04-07"
            def order = orderService.createOrder(orderReq, uowO)
            unitOfWorkService.commit(uowO)

        when: "the User's shippingAddress is updated"
            def uowR = unitOfWorkService.createUnitOfWork("sync-read-user")
            def loaded = userService.getUserById(user.aggregateId, uowR)
            loaded.shippingAddress = "999 New Place"
            def uowUpd = unitOfWorkService.createUnitOfWork("sync-update-user")
            userService.updateUser(loaded, uowUpd)
            unitOfWorkService.commit(uowUpd)

        then: "within ~5 seconds the Order's OrderUser projection reflects the new address"
            def found = waitForProjection(order.aggregateId, "999 New Place", 10_000)
            found != null
            found.user.shippingAddress == "999 New Place"
    }

    private def waitForProjection(Integer orderId, String expectedAddress, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            def uow = unitOfWorkService.createUnitOfWork("sync-poll")
            def reloaded = orderService.getOrderById(orderId, uow)
            if (reloaded != null && reloaded.user != null && expectedAddress == reloaded.user.shippingAddress) {
                return reloaded
            }
            Thread.sleep(250)
        }
        return null
    }
}
