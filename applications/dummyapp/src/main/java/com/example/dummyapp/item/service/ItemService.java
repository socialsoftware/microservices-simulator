package com.example.dummyapp.item.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
import com.example.dummyapp.item.aggregate.Item;
import com.example.dummyapp.item.aggregate.ItemDto;
import com.example.dummyapp.item.aggregate.ItemRepository;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ItemService(UnitOfWorkService unitOfWorkService, ItemRepository itemRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.itemRepository = itemRepository;
    }

    public ItemDto getItem(Integer itemAggregateId, UnitOfWork unitOfWork) {
        logger.info("Getting item {}", itemAggregateId);
        Item item = (Item) unitOfWorkService.aggregateLoadAndRegisterRead(itemAggregateId, unitOfWork);
        return new ItemDto(item);
    }

    public ItemDto createItem(ItemDto itemDto, UnitOfWork unitOfWork) {
        logger.info("Creating item {}", itemDto.getName());
        Item item = new Item(null, itemDto.getName(), itemDto.getPrice());
        unitOfWorkService.registerChanged(item, unitOfWork);
        return new ItemDto(item);
    }

    public ItemDto updateItem(Integer itemAggregateId, ItemDto itemDto, UnitOfWork unitOfWork) {
        logger.info("Updating item {}", itemAggregateId);
        Item item = (Item) unitOfWorkService.aggregateLoadAndRegisterRead(itemAggregateId, unitOfWork);
        item.setName(itemDto.getName());
        item.setPrice(itemDto.getPrice());
        this.unitOfWorkService.registerChanged(item, unitOfWork);
        return new ItemDto(item);
    }

    public void deleteItem(Integer itemAggregateId, UnitOfWork unitOfWork) {
        logger.info("Deleting item {}", itemAggregateId);
        Item item = (Item) unitOfWorkService.aggregateLoadAndRegisterRead(itemAggregateId, unitOfWork);
        item.remove();
        unitOfWorkService.registerChanged(item, unitOfWork);
    }
}
