package com.example.dummyapp

import com.example.dummyapp.item.aggregate.ItemDto
import com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas
import spock.lang.Specification

class RuntimeGateway {
    def loadExternalDto() {
        externalRuntime.fetchDto()
    }
}

class ItemDtoWrapper {
    def dto

    ItemDtoWrapper(dto) {
        this.dto = dto
    }

    def getDto() {
        dto
    }
}

class ItemBundle {
    def dto

    ItemBundle(dto) {
        this.dto = dto
    }

    def getDto() {
        dto
    }
}

class GroovySagaTracingSpec extends Specification {
    def orderSagaInField = new CreateOrderFunctionalitySagas(null, null)
    def plainAggregateInField = new DummyAggregate(200, 'plain')
    def runtimeGateway = new RuntimeGateway()

    def orderSagaInSetup
    static orderSagaInSetupSpec

    def setup() {
        def setupAlias = new CreateOrderFunctionalitySagas(null, null)
        orderSagaInSetup = setupAlias
        setupAlias.executeWorkflow(null)
    }

    def setupSpec() {
        def setupSpecAlias = new CreateOrderFunctionalitySagas(null, null)
        orderSagaInSetupSpec = setupSpecAlias
        setupSpecAlias.resumeWorkflow(null)
    }

    def 'same method tracks two order saga instances'() {
        given:
        def firstOrderSaga = new CreateOrderFunctionalitySagas(null, null)
        def secondOrderSaga = new CreateOrderFunctionalitySagas(null, null)

        when:
        firstOrderSaga.executeWorkflow(null)
        secondOrderSaga.executeUntilStep('createOrderStep', null)

        then:
        true
    }

    def 'helper chain and accessor provenance feed item saga constructor'() {
        given:
        def helperSaga = buildItemSagaFromBundle(buildItemBundle())

        when:
        helperSaga.executeWorkflow(null)

        then:
        true
    }

    def 'named args, setters, and toSet provenance feed item saga constructor'() {
        given:
        def namedDto = new ItemDto(aggregateId: 17, orderId: 23)
        def setterDto = new ItemDto()
        setterDto.setAggregateId(namedDto.getAggregateId())
        setterDto.setOrderId([1, 2, 3].toSet().size())
        def saga = new CreateItemFunctionalitySagas(null, setterDto, null, null)

        when:
        saga.resumeWorkflow(null)

        then:
        true
    }

    def 'try catch and retry loop include workflow calls'() {
        given:
        def retrySaga = new CreateOrderFunctionalitySagas(null, null)
        def retries = 2

        when:
        try {
            retrySaga.resumeWorkflow(null)
        } catch (Exception ignored) {
            retries = retries - 1
        }

        while (retries > 0) {
            try {
                retrySaga.executeWorkflow(null)
                retries = 0
            } catch (Exception ignored) {
                retries = retries - 1
            }
        }

        then:
        true
    }

    def 'for and do-while blocks include workflow calls'() {
        given:
        def loopSaga = new CreateOrderFunctionalitySagas(null, null)
        def retries = 0

        when:
        for (int i = 0; i < 1; i++) {
            loopSaga.executeUntilStep('createOrderStep', null)
        }

        do {
            loopSaga.resumeWorkflow(null)
            retries = retries + 1
        } while (retries < 1)

        then:
        true
    }

    def 'if switch and finally blocks include workflow calls'() {
        given:
        def branchSaga = new CreateOrderFunctionalitySagas(null, null)
        def branch = 'switch'

        when:
        if (branch == 'if') {
            branchSaga.executeWorkflow(null)
        } else {
            branchSaga.executeUntilStep('createOrderStep', null)
        }

        switch (branch) {
            case 'switch':
                branchSaga.resumeWorkflow(null)
                break
            default:
                branchSaga.executeWorkflow(null)
        }

        try {
            branch = 'done'
        } finally {
            branchSaga.executeWorkflow(null)
        }

        then:
        true
    }

    def 'runtime edge stays conservative for item saga input'() {
        given:
        def runtimeDto = runtimeGateway.loadExternalDto()
        def runtimeSaga = new CreateItemFunctionalitySagas(null, runtimeDto, null, null)

        when:
        runtimeSaga.executeWorkflow(null)

        then:
        true
    }

    def buildItemSagaFromBundle(bundle) {
        createItemSaga(bundle.dto)
    }

    def createItemSaga(dto) {
        new CreateItemFunctionalitySagas(null, dto, null, null)
    }

    def buildItemBundle() {
        new ItemBundle(buildItemDto())
    }

    def buildItemDto() {
        new ItemDtoWrapper(new ItemDto(aggregateId: 31, orderId: 7)).dto
    }
}

class GroovySagaTracingBaseSpec extends Specification {
    def inheritedSagaInField = new CreateOrderFunctionalitySagas(null, null)

    def setup() {
        def setupSaga = inheritedHelper()
        setupSaga.executeWorkflow(null)
    }

    def setupSpec() {
        def setupSpecSaga = inheritedHelper()
        setupSpecSaga.resumeWorkflow(null)
    }

    def inheritedHelper() {
        new CreateOrderFunctionalitySagas(null, null)
    }
}

class GroovySagaTracingChildSpec extends GroovySagaTracingBaseSpec {
    def 'child uses inherited helper saga'() {
        given:
        def inheritedSaga = inheritedHelper()

        when:
        inheritedSaga.executeWorkflow(null)

        then:
        true
    }
}

class GroovySagaTracingShadowChildSpec extends GroovySagaTracingBaseSpec {
    def inheritedSagaInField = new CreateOrderFunctionalitySagas(null, null)

    def 'child executes shadowed saga field'() {
        when:
        inheritedSagaInField.resumeWorkflow(null)

        then:
        true
    }
}
