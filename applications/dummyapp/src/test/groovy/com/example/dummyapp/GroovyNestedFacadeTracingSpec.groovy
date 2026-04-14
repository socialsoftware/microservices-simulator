package com.example.dummyapp

import com.example.dummyapp.item.aggregate.ItemDto
import com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
import com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import spock.lang.Specification

class GroovyNestedFacadeTracingSpec extends Specification {
    def itemFunctionalities = new ItemFunctionalitiesFacade()
    def itemDto = new ItemDto(aggregateId: 3, orderId: 5)

    def 'nested helper facade result feeds item saga constructor'() {
        given:
        itemFunctionalities.@sagaUnitOfWorkService = Stub(SagaUnitOfWorkService)
        itemFunctionalities.@commandGateway = Stub(CommandGateway) {
            send(_) >> new ItemDto(aggregateId: 51, orderId: 29)
        }
        def helperSaga = createItemSaga(buildItemDtoViaFacade())

        when:
        helperSaga.executeWorkflow(null)

        then:
        true
    }

    def 'shadowed helper field/local itemDto remains acyclic'() {
        given:
        itemFunctionalities.@sagaUnitOfWorkService = Stub(SagaUnitOfWorkService)
        itemFunctionalities.@commandGateway = Stub(CommandGateway) {
            send(_) >> new ItemDto(aggregateId: 67, orderId: 31)
        }
        def shadowSaga = createItemSaga(buildItemDtoWithShadowing())

        when:
        shadowSaga.executeWorkflow(null)

        then:
        true
    }

    def createItemSaga(dto) {
        new CreateItemFunctionalitySagas(null, dto, null, null)
    }

    def buildItemDto() {
        new ItemDto(aggregateId: 17, orderId: 11)
    }

    def buildItemDtoViaFacade() {
        def itemDto = buildItemDto()
        itemDto = itemFunctionalities.createItem(itemDto)
        itemDto
    }

    def buildItemDtoWithShadowing() {
        def itemDto = buildItemDto()
        if (itemDto != null) {
            itemDto = itemFunctionalities.createItem(itemDto)
        }
        itemDto
    }
}
