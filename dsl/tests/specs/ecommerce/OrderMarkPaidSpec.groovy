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
class OrderMarkPaidSpec extends Specification {

    @Autowired
    OrderService orderService

    @Autowired
    UserService userService

    @Autowired
    UnitOfWorkService unitOfWorkService

    def "markPaid: load + assign + auto register-changed flips status from PENDING to PAID"() {
        given: "a user exists"
            def uowUser = unitOfWorkService.createUnitOfWork("spec-create-user")
            def userReq = new CreateUserRequestDto()
            userReq.username = "alice"
            userReq.email = "alice@example.com"
            userReq.passwordHash = "hash"
            userReq.shippingAddress = "1 Test Street"
            def user = userService.createUser(userReq, uowUser)
            unitOfWorkService.commit(uowUser)

        and: "a PENDING order references that user"
            def uowOrder = unitOfWorkService.createUnitOfWork("spec-create-order")
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.username = user.username
            userRef.email = user.email
            userRef.shippingAddress = user.shippingAddress
            def orderReq = new CreateOrderRequestDto()
            orderReq.user = userRef
            orderReq.totalInCents = 4200.0d
            orderReq.itemCount = 1
            orderReq.status = OrderStatus.PENDING
            orderReq.placedAt = "2026-04-07"
            def order = orderService.createOrder(orderReq, uowOrder)
            unitOfWorkService.commit(uowOrder)

        when: "markPaid runs"
            def uowMark = unitOfWorkService.createUnitOfWork("spec-mark-paid")
            orderService.markPaid(order.aggregateId, uowMark)
            unitOfWorkService.commit(uowMark)

        then: "the persisted Order's status is now PAID"
            def uowRead = unitOfWorkService.createUnitOfWork("spec-read-back")
            def reloaded = orderService.getOrderById(order.aggregateId, uowRead)
            reloaded.status == "PAID"
    }

    def "markPaid: invoking on a non-existent order id throws"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("spec-mark-paid-missing")

        when:
            orderService.markPaid(999999, uow)

        then:
            thrown(Exception)
    }
}
