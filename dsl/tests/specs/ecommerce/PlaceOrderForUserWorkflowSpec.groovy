package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.sagas.PlaceOrderForUserFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.service.OrderService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class PlaceOrderForUserWorkflowSpec extends Specification {

    @Autowired CommandGateway commandGateway
    @Autowired SagaUnitOfWorkService unitOfWorkService
    @Autowired UserService userService
    @Autowired OrderService orderService

    def "the workflow chains a UserDto from step 1 into Order.placeOrder in step 2 and the projection is populated"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("place-order-for-user")
            def workflow = new PlaceOrderForUserFunctionalitySagas(
                unitOfWorkService,
                "frances", "frances@example.com", "10 Saga Lane",
                4500.0d, 3, "2026-04-07",
                uow, commandGateway)
        when:
            workflow.executeWorkflow(uow)
        then: "both aggregates are persisted"
            def uowR = unitOfWorkService.createUnitOfWork("place-order-for-user-read")
            def user = userService.getAllUsers(uowR).find { it.username == "frances" }
            user != null

            def order = orderService.getAllOrders(uowR).find {
                it.user != null && it.user.aggregateId == user.aggregateId
            }
            order != null

        and: "the OrderUser projection on the Order carries the values from the UserDto chained between steps"
            order.user.username == "frances"
            order.user.email == "frances@example.com"
            order.user.shippingAddress == "10 Saga Lane"
            order.totalInCents == 4500.0d
            order.itemCount == 3
            order.status == "PENDING"
    }
}
