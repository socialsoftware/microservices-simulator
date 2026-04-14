package pt.ulisboa.tecnico.socialsoftware.advanced

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ProductCrudSpec extends Specification {

    @Autowired ProductService productService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createProduct + getProductById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("adv-create")
            def req = new CreateProductRequestDto()
            req.name = "Widget"
            req.price = 99.99d
            req.available = true

        when:
            def created = productService.createProduct(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("adv-read")
            def reloaded = productService.getProductById(created.aggregateId, uowR)
            reloaded.name == "Widget"
            reloaded.price == 99.99d
    }
}
