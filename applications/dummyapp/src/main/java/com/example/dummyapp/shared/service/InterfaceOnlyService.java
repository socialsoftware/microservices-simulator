package com.example.dummyapp.shared.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;

@Service
public class InterfaceOnlyService implements InterfaceOnlyServiceApi {

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public InterfaceOnlyService(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    @Override
    public Object loadInterfaceOnly(Integer id, UnitOfWork unitOfWork) {
        return unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
    }
}
