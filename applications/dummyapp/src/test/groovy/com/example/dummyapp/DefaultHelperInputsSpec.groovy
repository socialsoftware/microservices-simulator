package com.example.dummyapp

import com.example.dummyapp.item.aggregate.ItemDto
import com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
import spock.lang.Specification

// Source-only fixture: these helpers model ordinary test inputs, not application changes.
class DefaultHelperInputsSpec extends Specification {
    static final String DEFAULT_NAME = 'default-item'

    def 'omitted defaults and explicit overrides'() {
        given:
        def defaults = new CreateItemFunctionalitySagas(null, dto(), null, null)
        def override = new CreateItemFunctionalitySagas(null, dto('chosen', 73), null, null)
        def partial = new CreateItemFunctionalitySagas(null, dto('partial'), null, null)
        def ambiguous = new CreateItemFunctionalitySagas(null, overloaded(), null, null)
        expect:
        true
    }

    ItemDto dto(String name = DEFAULT_NAME, Integer orderId = 41) {
        def result = new ItemDto()
        result.setName(name)
        result.setOrderId(orderId)
        return result
    }

    ItemDto overloaded(String name = 'a') { return dto(name) }
    ItemDto overloaded(Integer orderId = 42) { return dto('b', orderId) }
}
