package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.sagas.CompensatingChainFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CompensatingChainWorkflowSpec extends Specification {

    @Autowired CommandGateway commandGateway
    @Autowired SagaUnitOfWorkService unitOfWorkService
    @Autowired ProductService productService

    def "the second step's failure throws an exception"() {
        given: "a workflow whose second step is destined to fail"
            def uow = unitOfWorkService.createUnitOfWork("comp-run")
            def workflow = new CompensatingChainFunctionalitySagas(
                unitOfWorkService,
                "COMP-VALID-SKU", "", uow, commandGateway)
        when: "we run it with a valid sku followed by an empty (invariant-violating) one"
            workflow.executeWorkflow(uow)
        then: "the workflow rethrows the original failure"
            thrown(Exception)
    }

    def "the workflow runs cleanly when both steps succeed (no compensation runs)"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("comp-happy")
            def workflow = new CompensatingChainFunctionalitySagas(
                unitOfWorkService,
                "COMP-A", "COMP-B", uow, commandGateway)
        when:
            workflow.executeWorkflow(uow)
        then:
            noExceptionThrown()
        and:
            def uowRead = unitOfWorkService.createUnitOfWork("comp-happy-read")
            def all = productService.getAllProducts(uowRead)
            all.any { it.sku == "COMP-A" }
            all.any { it.sku == "COMP-B" }
    }
}
