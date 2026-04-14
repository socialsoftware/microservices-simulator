package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.sagas.SeedUserAndProductFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class WorkflowFailureModesSpec extends Specification {

    @Autowired CommandGateway commandGateway
    @Autowired SagaUnitOfWorkService unitOfWorkService
    @Autowired ProductService productService
    @Autowired UserService userService

    def "second-step product failure rolls back the first-step user via deleteUser compensation"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("wfail-rollback-user")
            def workflow = new SeedUserAndProductFunctionalitySagas(
                unitOfWorkService,
                "wfail-rollback-user", "wfail-rollback@example.com", "addr", "",
                uow, commandGateway)
        when: "the second step's product creation fails because sku is empty"
            workflow.executeWorkflow(uow)
        then:
            thrown(Exception)
        and: "the user the first step created has been compensated away"
            def uowR = unitOfWorkService.createUnitOfWork("wfail-rollback-user-read")
            def all = userService.getAllUsers(uowR)
            !all.any { it.username == "wfail-rollback-user" }
    }

    def "happy path leaves both user and product persisted"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("wfail-happy")
            def workflow = new SeedUserAndProductFunctionalitySagas(
                unitOfWorkService,
                "wfail-happy-user", "wfail-happy@example.com", "addr", "WFAIL-OK",
                uow, commandGateway)
        when:
            workflow.executeWorkflow(uow)
        then:
            noExceptionThrown()
        and:
            def uowR = unitOfWorkService.createUnitOfWork("wfail-happy-read")
            userService.getAllUsers(uowR).any { it.username == "wfail-happy-user" }
            productService.getAllProducts(uowR).any { it.sku == "WFAIL-OK" }
    }

    def "running the workflow twice with the same input produces two distinct user rows"() {
        given:
            def uow1 = unitOfWorkService.createUnitOfWork("wfail-idem-1")
            def w1 = new SeedUserAndProductFunctionalitySagas(
                unitOfWorkService,
                "wfail-idem-user", "wfail-idem@example.com", "addr", "WFAIL-IDEM-1",
                uow1, commandGateway)
        when:
            w1.executeWorkflow(uow1)
        and:
            def uow2 = unitOfWorkService.createUnitOfWork("wfail-idem-2")
            def w2 = new SeedUserAndProductFunctionalitySagas(
                unitOfWorkService,
                "wfail-idem-user", "wfail-idem@example.com", "addr", "WFAIL-IDEM-2",
                uow2, commandGateway)
            w2.executeWorkflow(uow2)
        then: "the DSL has no uniqueness on username today, so two rows exist"
            def uowR = unitOfWorkService.createUnitOfWork("wfail-idem-r")
            def matches = userService.getAllUsers(uowR).findAll { it.username == "wfail-idem-user" }
            matches.size() == 2
    }
}
