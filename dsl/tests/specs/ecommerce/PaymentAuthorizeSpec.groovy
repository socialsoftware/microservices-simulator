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
class PaymentAuthorizeSpec extends Specification {

    @Autowired
    PaymentService paymentService

    @Autowired
    UnitOfWorkService unitOfWorkService

    def "happy path: authorize creates a Payment with status AUTHORIZED"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("spec-happy")

        when:
            def dto = paymentService.authorize(42, 2500.0d, "VISA", uow)
            unitOfWorkService.commit(uow)

        then:
            dto != null
            dto.amountInCents == 2500.0d
            dto.paymentMethod == "VISA"
            dto.status == "AUTHORIZED"
            dto.authorizationCode == "AUTH-PENDING"
    }

    def "precondition rejects negative amount with the DSL-declared message"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("spec-bad")

        when:
            paymentService.authorize(42, -100.0d, "VISA", uow)

        then:
            def e = thrown(EcommerceException)
            e.message.contains("Payment amount must be positive")
    }
}
