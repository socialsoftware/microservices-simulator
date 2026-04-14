package pt.ulisboa.tecnico.socialsoftware.ecommerce

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service.PaymentService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class PaymentAuthorizedEventEmissionSpec extends Specification {

    @Autowired
    PaymentService paymentService

    @Autowired
    UnitOfWorkService unitOfWorkService

    @Autowired
    EventRepository eventRepository

    def "authorize publishes a PaymentAuthorizedEvent that is persisted in the event store"() {
        given:
            def beforeIds = eventRepository.findAll()
                .findAll { it instanceof PaymentAuthorizedEvent }
                .collect { it.id } as Set
            def uow = unitOfWorkService.createUnitOfWork("emit-spec")

        when:
            def dto = paymentService.authorize(7, 9999.0d, "VISA", uow)
            unitOfWorkService.commit(uow)

        then:
            dto != null

        and:
            def afterEvents = eventRepository.findAll()
                .findAll { it instanceof PaymentAuthorizedEvent }
                .findAll { !beforeIds.contains(it.id) }
            afterEvents.size() == 1
            def emitted = (PaymentAuthorizedEvent) afterEvents[0]
            emitted.orderAggregateId == 7
            emitted.amountInCents == 9999.0d
    }
}
