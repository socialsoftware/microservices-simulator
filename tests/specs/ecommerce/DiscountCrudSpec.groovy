package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos.CreateDiscountRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.service.DiscountService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class DiscountCrudSpec extends Specification {

    @Autowired DiscountService discountService
    @Autowired UnitOfWorkService unitOfWorkService

    private CreateDiscountRequestDto base(String code, double pct) {
        def req = new CreateDiscountRequestDto()
        req.code = code
        req.description = "promo"
        req.percentageOff = pct
        req.active = true
        req.validFrom = "2026-01-01"
        req.validUntil = "2026-12-31"
        return req
    }

    def "createDiscount persists and reads back"() {
        when:
            def uow = unitOfWorkService.createUnitOfWork("dcrud-create")
            def created = discountService.createDiscount(base("PROMO10", 10.0d), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("dcrud-read")
            def found = discountService.getDiscountById(created.aggregateId, uowR)
            found.code == "PROMO10"
            found.percentageOff == 10.0d
            found.active
    }

    def "codeNotEmpty rejects empty code"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("dcrud-empty")
        when:
            discountService.createDiscount(base("", 10.0d), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Discount code cannot be empty")
    }

    def "percentageInRange rejects negative percentage"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("dcrud-neg")
        when:
            discountService.createDiscount(base("PROMO-NEG", -5.0d), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Discount percentage must be between 0 and 100")
    }

    def "percentageInRange rejects above 100"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("dcrud-over")
        when:
            discountService.createDiscount(base("PROMO-OVER", 150.0d), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Discount percentage must be between 0 and 100")
    }

    def "boundary 0 and 100 are accepted"() {
        when:
            def uow = unitOfWorkService.createUnitOfWork("dcrud-edges")
            def zero = discountService.createDiscount(base("PROMO-0", 0.0d), uow)
            def hundred = discountService.createDiscount(base("PROMO-100", 100.0d), uow)
            unitOfWorkService.commit(uow)
        then:
            zero.percentageOff == 0.0d
            hundred.percentageOff == 100.0d
    }
}
