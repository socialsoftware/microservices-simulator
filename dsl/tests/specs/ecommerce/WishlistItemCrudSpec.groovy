package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.service.ProductService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos.CreateWishlistItemRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.service.WishlistItemService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ProductDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class WishlistItemCrudSpec extends Specification {

    @Autowired WishlistItemService wishlistItemService
    @Autowired UserService userService
    @Autowired ProductService productService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("wcrud-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "wcrud-$tag"
        req.email = "$tag@x.com"
        req.passwordHash = "h"
        req.shippingAddress = "addr"
        def u = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return u
    }

    private ProductDto seedProduct(String sku) {
        def uow = unitOfWorkService.createUnitOfWork("wcrud-prod-$sku")
        def req = new CreateProductRequestDto()
        req.sku = sku
        req.name = "Tea"
        req.description = "test"
        req.priceInCents = 1500.0d
        req.stock = 10
        def p = productService.createProduct(req, uow)
        unitOfWorkService.commit(uow)
        return p
    }

    private CreateWishlistItemRequestDto reqFor(UserDto u, ProductDto p, String addedAt) {
        def userRef = new UserDto()
        userRef.aggregateId = u.aggregateId
        userRef.username = u.username
        userRef.email = u.email
        userRef.shippingAddress = u.shippingAddress
        def prodRef = new ProductDto()
        prodRef.aggregateId = p.aggregateId
        prodRef.sku = p.sku
        prodRef.name = p.name
        prodRef.priceInCents = p.priceInCents
        def req = new CreateWishlistItemRequestDto()
        req.user = userRef
        req.product = prodRef
        req.addedAt = addedAt
        return req
    }

    def "createWishlistItem persists with both user and product projections"() {
        given:
            def u = seedUser("wcreate")
            def p = seedProduct("WSKU-CREATE")
        when:
            def uow = unitOfWorkService.createUnitOfWork("wcrud-create")
            def item = wishlistItemService.createWishlistItem(reqFor(u, p, "2026-04-08"), uow)
            unitOfWorkService.commit(uow)
        then:
            item != null
            item.user != null
            item.user.username == u.username
            item.product != null
            item.product.sku == p.sku
            item.addedAt == "2026-04-08"
    }

    def "addedAtNotEmpty rejects empty timestamp"() {
        given:
            def u = seedUser("wempty")
            def p = seedProduct("WSKU-EMPTY")
            def uow = unitOfWorkService.createUnitOfWork("wcrud-empty")
        when:
            wishlistItemService.createWishlistItem(reqFor(u, p, ""), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Wishlist item must have an added timestamp")
    }

    def "deleting a user that has wishlist items succeeds (reactive model)"() {
        given:
            def u = seedUser("wref")
            def p = seedProduct("WSKU-REF")
            def uow1 = unitOfWorkService.createUnitOfWork("wcrud-ref-c")
            wishlistItemService.createWishlistItem(reqFor(u, p, "2026-04-08"), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uowD = unitOfWorkService.createUnitOfWork("wcrud-ref-d")
            userService.deleteUser(u.aggregateId, uowD)
            unitOfWorkService.commit(uowD)
        then: "deletion succeeds — subscriber reacts asynchronously"
            noExceptionThrown()
    }

    def "deleting a product that is on a wishlist succeeds (reactive model)"() {
        given:
            def u = seedUser("wprefref")
            def p = seedProduct("WSKU-PREFREF")
            def uow1 = unitOfWorkService.createUnitOfWork("wcrud-pref-c")
            wishlistItemService.createWishlistItem(reqFor(u, p, "2026-04-08"), uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uowD = unitOfWorkService.createUnitOfWork("wcrud-pref-d")
            productService.deleteProduct(p.aggregateId, uowD)
            unitOfWorkService.commit(uowD)
        then: "deletion succeeds — subscriber reacts asynchronously"
            noExceptionThrown()
    }
}
