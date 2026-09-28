package com.example.dummyapp

import com.example.dummyapp.item.aggregate.ItemDto
import com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade
import com.example.dummyapp.order.coordination.OrderFunctionalitiesFacade
import spock.lang.Specification

// Source-only fixture: the shared result supplies arguments, but not all preparation.
class SharedSetupPreparationSpec extends Specification {
    def itemFunctionalities = new ItemFunctionalitiesFacade()
    def orderFunctionalities = new OrderFunctionalitiesFacade()
    ItemDto preparedItem

    def setup() {
        preparedItem = itemFunctionalities.createItem(new ItemDto(aggregateId: 901, orderId: 902))
    }

    def 'shared argument producer still requires the feature void effect'() {
        given:
        orderFunctionalities.createOrder(903)

        when:
        itemFunctionalities.createItem(preparedItem)

        then:
        true
    }

    def 'shared argument producer cannot bypass a feature preparation barrier'() {
        given:
        orderFunctionalities.createOrder(903)
        if (true) {
            def marker = 'branched'
        }

        when:
        itemFunctionalities.createItem(preparedItem)

        then:
        true
    }
}
