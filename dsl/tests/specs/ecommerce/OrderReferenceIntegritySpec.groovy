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
class OrderReferenceIntegritySpec extends Specification {

    @Autowired OrderService orderService
    @Autowired UserService userService
    @Autowired UnitOfWorkService unitOfWorkService

    def "deleteUser succeeds even when an Order references the user (reactive model)"() {
        given: "a user with an order"
            def uowU = unitOfWorkService.createUnitOfWork("ori-create-u")
            def uReq = new CreateUserRequestDto()
            uReq.username = "ori-fred"
            uReq.email = "fred@x.com"
            uReq.passwordHash = "h"
            uReq.shippingAddress = "addr"
            def user = userService.createUser(uReq, uowU)
            unitOfWorkService.commit(uowU)
        and:
            def uowO = unitOfWorkService.createUnitOfWork("ori-create-o")
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.username = user.username
            userRef.email = user.email
            userRef.shippingAddress = user.shippingAddress
            def oReq = new CreateOrderRequestDto()
            oReq.user = userRef
            oReq.totalInCents = 100.0d
            oReq.itemCount = 1
            oReq.status = OrderStatus.PENDING
            oReq.placedAt = "2026-04-07"
            orderService.createOrder(oReq, uowO)
            unitOfWorkService.commit(uowO)
        when: "we delete the user"
            def uowD = unitOfWorkService.createUnitOfWork("ori-del-u")
            userService.deleteUser(user.aggregateId, uowD)
            unitOfWorkService.commit(uowD)
        then: "the deletion succeeds (no synchronous blocking)"
            noExceptionThrown()
    }

    def "creating an Order with a non-existent user fails"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("ori-ghost")
            def ghost = new UserDto()
            ghost.aggregateId = 9_999_999
            ghost.username = "ghost"
            ghost.email = "ghost@x.com"
            ghost.shippingAddress = "nowhere"
            def oReq = new CreateOrderRequestDto()
            oReq.user = ghost
            oReq.totalInCents = 100.0d
            oReq.itemCount = 1
            oReq.status = OrderStatus.PENDING
            oReq.placedAt = "2026-04-07"
        when:
            orderService.createOrder(oReq, uow)
            unitOfWorkService.commit(uow)
        then:
            thrown(Exception)
    }
}
