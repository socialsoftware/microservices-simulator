package com.example.dummyapp.item.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import com.example.dummyapp.item.aggregate.Item;

@Service
public class AliasUnitOfWorkItemService {

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public AliasUnitOfWorkItemService(UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void aliasRegisterChanged(Item item, UnitOfWork unitOfWork) {
        var tracker = unitOfWorkService;
        tracker.registerChanged(item, unitOfWork);
    }
}
