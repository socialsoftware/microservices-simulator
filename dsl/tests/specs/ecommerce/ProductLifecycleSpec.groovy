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
class ProductLifecycleSpec extends Specification {

    @Autowired ProductService productService
    @Autowired UnitOfWorkService unitOfWorkService

    private Integer createProduct(String sku, double price, int stock) {
        def uow = unitOfWorkService.createUnitOfWork("plife-create-$sku")
        def req = new CreateProductRequestDto()
        req.sku = sku
        req.name = "n"
        req.description = "d"
        req.priceInCents = price
        req.stock = stock
        def dto = productService.createProduct(req, uow)
        unitOfWorkService.commit(uow)
        return dto.aggregateId
    }

    def "update that violates priceNonNegative is rejected with the DSL message"() {
        given:
            def id = createProduct("PLIFE-PRICE", 100.0d, 1)
        when:
            def uowR = unitOfWorkService.createUnitOfWork("plife-pr-r")
            def existing = productService.getProductById(id, uowR)
            existing.priceInCents = -50.0d
            def uowU = unitOfWorkService.createUnitOfWork("plife-pr-u")
            productService.updateProduct(existing, uowU)
            unitOfWorkService.commit(uowU)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Product price cannot be negative")
    }

    def "update that violates stockNonNegative is rejected"() {
        given:
            def id = createProduct("PLIFE-STOCK", 100.0d, 1)
        when:
            def uowR = unitOfWorkService.createUnitOfWork("plife-st-r")
            def existing = productService.getProductById(id, uowR)
            existing.stock = -1
            def uowU = unitOfWorkService.createUnitOfWork("plife-st-u")
            productService.updateProduct(existing, uowU)
            unitOfWorkService.commit(uowU)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Product stock cannot be negative")
    }

    def "deleting a product twice is harmless or throws — but never silently corrupts state"() {
        given:
            def id = createProduct("PLIFE-DEL2", 1.0d, 1)
        when:
            def uow1 = unitOfWorkService.createUnitOfWork("plife-d2-1")
            productService.deleteProduct(id, uow1)
            unitOfWorkService.commit(uow1)
        and:
            def uow2 = unitOfWorkService.createUnitOfWork("plife-d2-2")
            try {
                productService.deleteProduct(id, uow2)
                unitOfWorkService.commit(uow2)
            } catch (Exception ignored) {
            }
        then:
            def uowR = unitOfWorkService.createUnitOfWork("plife-d2-r")
            def all = productService.getAllProducts(uowR)
            !all.any { it.sku == "PLIFE-DEL2" }
    }
}
