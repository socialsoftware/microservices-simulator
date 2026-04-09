package com.example.dummyapp

import spock.lang.Specification

class DummyAppSpec extends Specification {

    def 'dummy aggregate is created'() {
        when:
        def agg = new DummyAggregate(1, 'test')

        then:
        agg.aggregateId == 1
        agg.label == 'test'
    }
}
