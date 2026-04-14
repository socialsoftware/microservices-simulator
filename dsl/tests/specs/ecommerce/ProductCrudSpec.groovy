package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ProductCrudSpec extends Specification {

    @Autowired
    ProductService productService

    @Autowired
    UnitOfWorkService unitOfWorkService

    private Integer createProduct(String sku, String name, double price, int stock) {
        def uow = unitOfWorkService.createUnitOfWork("spec-create-$sku")
        def req = new CreateProductRequestDto()
        req.sku = sku
        req.name = name
        req.description = "test description"
        req.priceInCents = price
        req.stock = stock
        def dto = productService.createProduct(req, uow)
        unitOfWorkService.commit(uow)
        return dto.aggregateId
    }

    def "createProduct persists a row that getProductById can read back"() {
        when:
            def id = createProduct("SKU-001", "Tea", 1500.0d, 10)

        then:
            def uow = unitOfWorkService.createUnitOfWork("spec-read")
            def found = productService.getProductById(id, uow)
            found.sku == "SKU-001"
            found.name == "Tea"
            found.priceInCents == 1500.0d
            found.stock == 10
    }

    def "getAllProducts returns every non-deleted row"() {
        given:
            createProduct("SKU-A", "Coffee", 2000.0d, 5)
            createProduct("SKU-B", "Espresso", 2500.0d, 8)

        when:
            def uow = unitOfWorkService.createUnitOfWork("spec-list")
            def all = productService.getAllProducts(uow)

        then:
            all.size() >= 2
            all.any { it.sku == "SKU-A" }
            all.any { it.sku == "SKU-B" }
    }

    def "updateProduct mutates the persisted row"() {
        given:
            def id = createProduct("SKU-U", "Original", 1000.0d, 1)

        when:
            def uowGet = unitOfWorkService.createUnitOfWork("spec-update-get")
            def existing = productService.getProductById(id, uowGet)
            existing.name = "Renamed"
            existing.priceInCents = 1234.5d
            def uowPut = unitOfWorkService.createUnitOfWork("spec-update-put")
            productService.updateProduct(existing, uowPut)
            unitOfWorkService.commit(uowPut)

        then:
            def uowRead = unitOfWorkService.createUnitOfWork("spec-update-read")
            def reloaded = productService.getProductById(id, uowRead)
            reloaded.name == "Renamed"
            reloaded.priceInCents == 1234.5d
    }

    def "deleteProduct hides the row from subsequent reads"() {
        given:
            def id = createProduct("SKU-D", "Doomed", 999.0d, 1)

        when:
            def uowDel = unitOfWorkService.createUnitOfWork("spec-delete")
            productService.deleteProduct(id, uowDel)
            unitOfWorkService.commit(uowDel)

        then:
            def uowRead = unitOfWorkService.createUnitOfWork("spec-deleted-read")
            try {
                productService.getProductById(id, uowRead)
                assert false : "expected getProductById to throw or return a DELETED row"
            } catch (Exception ignored) {
            }
    }
}
