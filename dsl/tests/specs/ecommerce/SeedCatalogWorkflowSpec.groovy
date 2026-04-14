package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.sagas.SeedCatalogFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class SeedCatalogWorkflowSpec extends Specification {

    @Autowired CommandGateway commandGateway
    @Autowired SagaUnitOfWorkService unitOfWorkService
    @Autowired ProductService productService

    def "the workflow creates two products from the same sku input"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("catalog-run")
            def workflow = new SeedCatalogFunctionalitySagas(
                unitOfWorkService,
                "CATALOG-SKU", uow, commandGateway)
        when:
            workflow.executeWorkflow(uow)
        then:
            noExceptionThrown()
    }
}
