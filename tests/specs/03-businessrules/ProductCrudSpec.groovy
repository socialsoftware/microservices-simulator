package pt.ulisboa.tecnico.socialsoftware.businessrules

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ProductCrudSpec extends Specification {

    @Autowired ProductService productService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createProduct + getProductById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("rules-create")
            def req = new CreateProductRequestDto()
            req.name = "Tea"
            req.sku = "TEA-001"
            req.price = 1500.0d
            req.stockQuantity = 5
            req.active = true

        when:
            def created = productService.createProduct(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("rules-read")
            def reloaded = productService.getProductById(created.aggregateId, uowR)
            reloaded.sku == "TEA-001"
            reloaded.price == 1500.0d
            reloaded.stockQuantity == 5
    }
}
