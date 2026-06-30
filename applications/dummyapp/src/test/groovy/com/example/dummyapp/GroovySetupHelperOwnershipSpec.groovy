package com.example.dummyapp

import com.example.dummyapp.item.aggregate.ItemDto
import com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

class GroovySetupHelperOwnershipSpec extends Specification {
    def itemFunctionalities = new ItemFunctionalitiesFacade()
    ItemDto setupItem

    def setup() {
        itemFunctionalities.@sagaUnitOfWorkService = Stub(SagaUnitOfWorkService)
        itemFunctionalities.@commandGateway = Stub(CommandGateway) {
            send(_) >> new ItemDto(aggregateId: 101, orderId: 201)
        }
        setupItem = createSetupItem()
    }

    def 'first feature depends on setup helper item'() {
        when:
        def featureItem = itemFunctionalities.createItem(new ItemDto(aggregateId: 103, orderId: 203))

        then:
        setupItem != null
        featureItem != null
    }

    def 'second feature depends on setup helper item'() {
        when:
        def featureItem = itemFunctionalities.createItem(new ItemDto(aggregateId: 104, orderId: 204))

        then:
        setupItem.aggregateId == 101
        featureItem != null
    }

    def 'direct feature item creation remains feature under test'() {
        when:
        def directItem = itemFunctionalities.createItem(new ItemDto(aggregateId: 102, orderId: 202))

        then:
        directItem != null
    }

    def 'feature calls helper that creates item'() {
        when:
        def helperItem = createItemFromFeatureHelper()

        then:
        helperItem != null
    }

    def createSetupItem() {
        itemFunctionalities.createItem(new ItemDto(aggregateId: 100, orderId: 200))
    }

    def createItemFromFeatureHelper() {
        itemFunctionalities.createItem(new ItemDto(aggregateId: 105, orderId: 205))
    }
}
