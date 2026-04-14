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
class SeedUserAndProductWorkflowSpec extends Specification {

    @Autowired CommandGateway commandGateway
    @Autowired SagaUnitOfWorkService unitOfWorkService
    @Autowired ProductService productService
    @Autowired UserService userService

    def "the workflow creates both a user and a product"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("seed-happy")
            def workflow = new SeedUserAndProductFunctionalitySagas(
                unitOfWorkService,
                "seed-alice", "alice@seed.com", "addr", "SEED-SKU-1",
                uow, commandGateway)
        when:
            workflow.executeWorkflow(uow)
        then:
            noExceptionThrown()
        and:
            def uowR = unitOfWorkService.createUnitOfWork("seed-happy-read")
            userService.getAllUsers(uowR).any { it.username == "seed-alice" }
            productService.getAllProducts(uowR).any { it.sku == "SEED-SKU-1" }
    }
}
