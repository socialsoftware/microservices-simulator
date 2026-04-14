package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.SagaPayment
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CustomEventSubscribeLimitationSpec extends Specification {

    def "Payment.getEventSubscriptions() does not register custom non-projection subscribes"() {
        given: "an in-memory Payment instance with the active state"
            def payment = new SagaPayment()
            payment.setStatus(PaymentStatus.AUTHORIZED)
            payment.setState(Payment.AggregateState.ACTIVE)

        when:
            def subs = payment.getEventSubscriptions()

        then: "no subscription targets OrderPlacedEvent"
            subs.findAll { it.getEventType() == "OrderPlacedEvent" }.isEmpty()

        and: "no subscription targets OrderCancelledEvent"
            subs.findAll { it.getEventType() == "OrderCancelledEvent" }.isEmpty()
    }
}
