package com.example.dummyapp.item.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
import com.example.dummyapp.item.aggregate.Item;
import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.aggregate.ItemRepository;

@Service
public class OverloadedItemService {

    private final ItemRepository itemRepository;
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public OverloadedItemService(UnitOfWorkService unitOfWorkService, ItemRepository itemRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.itemRepository = itemRepository;
    }

    public ItemDto processItem(ItemDto itemDto, UnitOfWork unitOfWork) {
        Item item = new Item(null, itemDto.getName(), itemDto.getPrice());
        unitOfWorkService.registerChanged(item, unitOfWork);
        return new ItemDto(item);
    }

    public ItemDto processItem(Integer itemId, UnitOfWork unitOfWork) {
        Item item = (Item) unitOfWorkService.aggregateLoadAndRegisterRead(itemId, unitOfWork);
        return new ItemDto(item);
    }
}
