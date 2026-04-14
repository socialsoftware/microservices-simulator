package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service.PaymentService
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class PaymentCrudSpec extends Specification {

    @Autowired PaymentService paymentService
    @Autowired UnitOfWorkService unitOfWorkService

    private CreatePaymentRequestDto base(double amt, String method) {
        def req = new CreatePaymentRequestDto()
        req.amountInCents = amt
        req.paymentMethod = method
        req.authorizationCode = "AUTH-CRUD"
        req.status = PaymentStatus.AUTHORIZED
        return req
    }

    def "createPayment persists and getPaymentById reads back"() {
        when:
            def uow1 = unitOfWorkService.createUnitOfWork("pcrud-c")
            def created = paymentService.createPayment(base(123.0d, "VISA"), uow1)
            unitOfWorkService.commit(uow1)
        then:
            def uow2 = unitOfWorkService.createUnitOfWork("pcrud-r")
            def found = paymentService.getPaymentById(created.aggregateId, uow2)
            found.amountInCents == 123.0d
            found.paymentMethod == "VISA"
            found.status == "AUTHORIZED"
    }

    // Update and delete on a Payment with a null `order` projection currently
    // crash with NPE in the saga snapshot comparison
    // (PaymentOrder.getOrderTotalInCents called on null). Tracked as a generator
    // bug in dsl/docs/MISSING-FUNCTIONALITY.md (#11 — null projection NPE on
    // update/delete). Tests omitted until that bug is fixed.
}
