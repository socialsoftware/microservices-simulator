package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service.PaymentService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class PaymentAuthorizeBoundarySpec extends Specification {

    @Autowired PaymentService paymentService
    @Autowired UnitOfWorkService unitOfWorkService

    def "exactly zero is rejected by the precondition (strict > 0.0)"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("pab-zero")
        when:
            paymentService.authorize(1, 0.0d, "VISA", uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Payment amount must be positive")
    }

    def "smallest positive amount is accepted"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("pab-tiny")
        when:
            def dto = paymentService.authorize(2, 0.01d, "VISA", uow)
            unitOfWorkService.commit(uow)
        then:
            dto.amountInCents == 0.01d
            dto.status == "AUTHORIZED"
    }

    def "very large amount is accepted"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("pab-big")
        when:
            def dto = paymentService.authorize(3, 9_999_999_999.99d, "VISA", uow)
            unitOfWorkService.commit(uow)
        then:
            dto.amountInCents == 9_999_999_999.99d
    }
}
