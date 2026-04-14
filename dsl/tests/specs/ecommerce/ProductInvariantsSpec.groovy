package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ProductInvariantsSpec extends Specification {

    @Autowired
    ProductService productService

    @Autowired
    UnitOfWorkService unitOfWorkService

    private CreateProductRequestDto base(String sku, double price, int stock) {
        def req = new CreateProductRequestDto()
        req.sku = sku
        req.name = "Tea"
        req.description = "test"
        req.priceInCents = price
        req.stock = stock
        return req
    }

    def "skuNotEmpty rejects empty sku with the DSL message"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-sku")

        when:
            productService.createProduct(base("", 1.0d, 1), uow)

        then:
            def e = thrown(EcommerceException)
            e.message.contains("Product SKU cannot be empty")
    }

    def "priceNonNegative rejects negative price with the DSL message"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-price")

        when:
            productService.createProduct(base("SKU-X", -1.0d, 1), uow)

        then:
            def e = thrown(EcommerceException)
            e.message.contains("Product price cannot be negative")
    }

    def "stockNonNegative rejects negative stock with the DSL message"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-stock")

        when:
            productService.createProduct(base("SKU-Y", 1.0d, -5), uow)

        then:
            def e = thrown(EcommerceException)
            e.message.contains("Product stock cannot be negative")
    }
}
