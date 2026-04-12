package com.example.dummyapp.item.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
import com.example.dummyapp.item.aggregate.Item;

@Service
public class HelperMarkChangedItemService {

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public HelperMarkChangedItemService(UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void helperRegisterChanged(Item item, UnitOfWork unitOfWork) {
        markChanged(item, unitOfWork);
    }

    private void markChanged(Item item, UnitOfWork unitOfWork) {
        unitOfWorkService.registerChanged(item, unitOfWork);
    }
}
