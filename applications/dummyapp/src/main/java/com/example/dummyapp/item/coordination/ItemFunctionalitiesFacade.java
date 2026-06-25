package com.example.dummyapp.item.coordination;

import com.example.dummyapp.events.ItemRenamedEvent;
import com.example.dummyapp.item.aggregate.ItemDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class ItemFunctionalitiesFacade {

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public ItemDto createItem(ItemDto itemDto) {
        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("createItem");
        CreateItemFunctionalitySagas functionality = new CreateItemFunctionalitySagas(
                sagaUnitOfWorkService,
                itemDto,
                unitOfWork,
                commandGateway);
        functionality.executeWorkflow(unitOfWork);
        return functionality.getItemDto();
    }

    public void renameItemFromEvent(Integer itemAggregateId, ItemRenamedEvent event) {
        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("renameItemFromEvent");
        RenameItemFromEventFunctionalitySagas functionality = new RenameItemFromEventFunctionalitySagas(
                sagaUnitOfWorkService,
                itemAggregateId,
                event.getUpdatedName(),
                event.getPublisherAggregateId(),
                event.getPublisherAggregateVersion(),
                unitOfWork,
                commandGateway);
        functionality.executeWorkflow(unitOfWork);
    }
}
