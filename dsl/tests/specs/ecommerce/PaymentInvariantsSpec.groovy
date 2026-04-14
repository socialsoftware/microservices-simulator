package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service.PaymentService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class PaymentInvariantsSpec extends Specification {

    @Autowired PaymentService paymentService
    @Autowired UnitOfWorkService unitOfWorkService

    private CreatePaymentRequestDto base(double amount, String method) {
        def req = new CreatePaymentRequestDto()
        req.amountInCents = amount
        req.paymentMethod = method
        req.authorizationCode = "AUTH-X"
        req.status = PaymentStatus.AUTHORIZED
        return req
    }

    def "amountPositive rejects zero amount on create"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("pinv-zero")
        when:
            paymentService.createPayment(base(0.0d, "VISA"), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Payment amount must be positive")
    }

    def "amountPositive rejects negative amount on create"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("pinv-neg")
        when:
            paymentService.createPayment(base(-1.0d, "VISA"), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Payment amount must be positive")
    }

    def "methodNotEmpty rejects empty payment method on create"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("pinv-method")
        when:
            paymentService.createPayment(base(100.0d, ""), uow)
        then:
            def e = thrown(EcommerceException)
            e.message.contains("Payment method cannot be empty")
    }
}
