package com.example.dummyapp

import com.example.dummyapp.order.coordination.OrderFunctionalitiesFacade
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

@ActiveProfiles('tcc')
class GroovyTccSourceModeTracingSpec extends Specification {
    def orderFunctionalities = new OrderFunctionalitiesFacade()

    def 'tcc profile facade call is rejected from saga catalog'() {
        given:
        orderFunctionalities.@sagaUnitOfWorkService = Stub(SagaUnitOfWorkService)

        when:
        orderFunctionalities.createOrder(99)

        then:
        true
    }
}
