package com.example.dummyapp.item.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import com.example.dummyapp.item.aggregate.Item;

@Service
public class GetterBasedUnitOfWorkItemService {

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public GetterBasedUnitOfWorkItemService(UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void getterRegisterChanged(Item item, UnitOfWork unitOfWork) {
        getUnitOfWorkService().registerChanged(item, unitOfWork);
    }

    private UnitOfWorkService<UnitOfWork> getUnitOfWorkService() {
        return unitOfWorkService;
    }
}
